package EasyRaft.ServerIO;

import EasyRaft.StateManager;
import EasyRaft.requests.MemberUpRequest;
import EasyRaft.state.CommitCallback;
import EasyRaft.state.State;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import sun.nio.ch.Net;

import java.util.*;

/**
 * Created by jrj on 17-12-28.
 */
//这一层keeper层构建在Raft之上,通过raft的接口运作
public class RaftKeeper {
    private static StateManager stateManager;
    public static void setStateManager(StateManager stateManager0){
        stateManager = stateManager0;
    }


    public static final char SelectLeaderRequest = '1';
    public static final char SelectLeaderResponse = '5';

    public static final char NotifyLeaderDown = '4';

    public static final char NotifyMemberDown = '7';
    public static final char NotifyMemberUp = '8';

    public static final char SetSlotRequest = '2';
    public static final char SetSlotResponse= '6';

    public static final char QuerySlotRequest = 'a';
    public static final char QuerySlotResponse = 'b';

    public static final char QueryAliveRequest = 'c';
    public static final char QueryAliveResponse = 'd';

    public static final char JoinClusterRequest = 'e';
    public static final char JoinClusterResponse = 'f';

    public static final char LeaveClusterRequest = 'g';
    public static final char LeaveClusterResponse = 'h';

    public static final char SayHelloRequest = 'i';
    public static String processRequest(ChannelHandlerContext ctx,ByteBuf byteBuf){
        char c = (char) byteBuf.readByte();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        if (c==SayHelloRequest){
            processSayHello(ctx,new String(bytes));
            byteBuf.release();
            return null;
        }
        if(!stateManager.isLeader()){
            byteBuf.release();
            return null;
        }
        switch (c){
            case SelectLeaderRequest:
                processSelectLeader(ctx,new String(bytes));
                break;
            case SetSlotRequest:
                if (!currentLeader.address.equals("null")){
                    processSetSlot(ctx,new String(bytes));
                }
                break;
            case JoinClusterRequest:
                if (!currentLeader.address.equals("null")){
                    processJoinCluster(ctx,new String(bytes));
                }
                break;
            case LeaveClusterRequest:
                if (!currentLeader.address.equals("null")){
                    processLeaveCluster(ctx,new String(bytes));
                }
                break;
            case QueryAliveRequest:
                if (!currentLeader.address.equals("null")){
                    processAliveQuery(ctx,new String(bytes));
                }
                break;
            case QuerySlotRequest:
                if (!currentLeader.address.equals("null")){
                    processSlotQuery(ctx,new String(bytes));
                }
                break;
        }
        return null;
    }

    private static CurrentLeader currentLeader = CurrentLeader.nullLeader;
    private static HashSet<ChannelHandlerContext> leaderWatcher = new HashSet<ChannelHandlerContext>();

    private static ArrayList<String> slot = new ArrayList<String>();

    static Hashtable<String,ChannelHandlerContext> strCtxMap = new Hashtable<String,ChannelHandlerContext>();
    static Hashtable<ChannelHandlerContext,String> ctxStrMap = new Hashtable<ChannelHandlerContext,String>();

    private static void processLeaveCluster(final ChannelHandlerContext ctx,final String request){
        final String entryId = "leaveCluster:" + request;
        Runnable callBack = new Runnable() {
            public void run() {
                String[] info = request.split("\\|");
                aliveList.remove(info[1]);
                System.out.println("committed leave cluster " + info[1]);
                ByteBuf byteBuf = getInfoWrapped(request,LeaveClusterResponse);
                ctx.writeAndFlush(byteBuf);
                /*
                for(Map.Entry<ChannelHandlerContext,String> entry:ctxStrMap.entrySet()){
                    if (entry.getValue().equals(info[1])){
                        aliveList.remove(entry.getKey());
                        //ctxStrMap.remove(entry.getKey());
                        ByteBuf byteBuf = getInfoWrapped(request,LeaveClusterResponse);
                        ctx.writeAndFlush(byteBuf);
                        break;
                    }
                }
                */
            }
        };
        //将回调函数传递给stateManager
        stateManager.commit(entryId,callBack);
    }

    private static void processAliveQuery(final ChannelHandlerContext ctx,final String request){
        //理论上这种read类的request是不用commit的,但是这里之所以要commit是因为防止脑裂的时候有Query产生.这样无法成功commit的地方,就不会有reply返回
        final String entryId = "QueryAlive";
        Runnable callBack = new Runnable() {
            public void run() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(request);
                for(String tmp:aliveList){
                    stringBuilder.append('|').append(tmp);
                }
                ByteBuf byteBuf = getInfoWrapped(stringBuilder.toString(),QueryAliveResponse);
                ctx.writeAndFlush(byteBuf);
            }
        };
        //将回调函数传递给stateManager
        stateManager.commit(entryId,callBack);
    }

    private static void processSlotQuery(final ChannelHandlerContext ctx,final String request){
        final String entryId = "QuerySlot";
        Runnable callBack = new Runnable() {
            public void run() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(request);
                for(String tmp:slot){
                    stringBuilder.append('|').append(tmp);
                }
                ByteBuf byteBuf = getInfoWrapped(stringBuilder.toString(),QuerySlotResponse);
                ctx.writeAndFlush(byteBuf);
            }
        };
        //将回调函数传递给stateManager
        stateManager.commit(entryId,callBack);
    }
    private static HashSet<String> aliveList = new HashSet<String>();
    private static class RaftKeeperCommit implements CommitCallback{
        public void executeAfterCommit(String log) {
            System.out.println(log);
            int position = log.indexOf(':');
            if(position<0){
                return;
            }
            String type = log.substring(0,position);
            String trueLog = log.substring(position+1);
            switch (type.charAt(0)){
                case 'L':
                    String[] leaderInfo = trueLog.split("\\|");
                    int epoch = Integer.parseInt(leaderInfo[1]);
                    if (epoch>currentLeader.epoch){
                        currentLeader = new CurrentLeader(epoch,leaderInfo[2],null);
                        System.out.println("current leader " + leaderInfo[2]);
                    }
                    break;
                case 'J':
                    aliveList.add(trueLog.split("\\|")[1]);
                    break;
                case 'S':
                    String[] slotInfo = trueLog.split("\\|");
                    int idx = Integer.parseInt(slotInfo[1]);
                    for (int i=slot.size();i<idx+1;i++){
                        slot.add("null");
                    }
                    slot.set(idx,slotInfo[2]);
                    System.out.println("set slot " + idx + " " + slotInfo[2]);
                    break;
            }
        }
    }

    static{
        State.setCommitCallback(new RaftKeeperCommit());
    }

    private static void processSayHello(final ChannelHandlerContext ctx,final String request){
        //只有是leader才发
        if(stateManager.isLeader()){
            if(request.equals(currentLeader.address)){
                currentLeader.ctx = ctx;
            }
            if(currentLeader.address.equals("null")){
                ByteBuf byteBuf = getInfoWrapped(currentLeader.epoch+"",NotifyLeaderDown);
                ctx.writeAndFlush(byteBuf);
                return;
            }
            if(currentLeader.ctx == ctx){
                for(String tmp:aliveList){
                    ChannelHandlerContext tmpCTX = strCtxMap.get(tmp);
                    //这里可能因为aliveList中加入了ctx,而ctxStrMap,strCtxMap没有来得及加入ctx导致错误的memberDown
                    if(!leaderWatcher.contains(tmpCTX)){
                        if(tmpCTX == null){
                            System.out.println("null tmpCtx");
                        }
                        System.out.println("tmp is " + tmp);
                        System.out.println("request is " + request);
                        ByteBuf byteBuf = getInfoWrapped(tmp,NotifyMemberDown);
                        currentLeader.ctx.writeAndFlush(byteBuf);
                    }
                }
            }
        }
    }

    private static void processSelectLeader(final ChannelHandlerContext ctx,final String request){
        final String entryId = "LeaderSelect:" + request;
        //这个runnable只有在成功commit后才会调用,重新将任务放到netty的进程中,防止race condition
        //并且由于raft是单线程,netty也用了单线程.raft中顺序执行的任务,他的回调,放到netty中也是顺序执行的.
        Runnable callBack = new Runnable() {
            public void run() {
                ctx.executor().submit(new Runnable() {
                    public void run() {
                        String[] info = request.split("\\|");
                        int epoch = Integer.parseInt(info[1]);
                        if (epoch>currentLeader.epoch){
                            currentLeader = new CurrentLeader(epoch,info[2],ctx);
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(info[0]).append('|');
                        stringBuilder.append(currentLeader.epoch).append('|');
                        stringBuilder.append(currentLeader.address);
                        ByteBuf responseBuffer = getInfoWrapped(stringBuilder.toString(),SelectLeaderResponse);
                        ctx.writeAndFlush(responseBuffer);
                    }
                });
            }
        };
        //将回调函数传递给stateManager
        stateManager.commit(entryId,callBack);
    }

    private static void processSetSlot(final ChannelHandlerContext ctx,final String request){
        final String entryId = "SetSlot:" + request;
        //这个runnable只有在成功commit后才会调用,重新将任务放到netty的进程中,防止race condition!
        Runnable callBack = new Runnable() {
            public void run() {
                //老实说,像这样嵌套的回调真的挺难受的.
                ctx.executor().submit(new Runnable() {
                    public void run() {
                        String[] info = request.split("\\|");
                        int idx = Integer.parseInt(info[1]);
                        for (int i=slot.size();i<idx+1;i++){
                            slot.add("null");
                        }
                        slot.set(idx,info[2]);
                        ByteBuf responseBuffer = getInfoWrapped(request,SetSlotResponse);
                        ctx.writeAndFlush(responseBuffer);
                    }
                });
            }
        };
        //将回调函数传递给stateManager
        stateManager.commit(entryId,callBack);
    }

    private static void processJoinCluster(final ChannelHandlerContext ctx,final String request){
        final String entryId = "JoinCluster:" + request;
        //这个runnable只有在成功commit后才会调用,重新将任务放到netty的进程中,防止race condition!
        Runnable callBack = new Runnable() {
            public void run() {
                //将任务放回netty线程,避免race condition
                ctx.executor().submit(new Runnable() {
                    public void run() {
                        String[] info = request.split("\\|");
                        //成功了之后在各种数据结构中加入相应的信息,以维持状态
                        //但是由于
                        aliveList.add(info[1]);
                        strCtxMap.put(info[1],ctx);
                        ctxStrMap.put(ctx,info[1]);
                        leaderWatcher.add(ctx);

                        if (currentLeader.ctx!= null){
                            //通知leader有人上线
                            ByteBuf byteBuf =getInfoWrapped(info[1], NotifyMemberUp);
                            currentLeader.ctx.writeAndFlush(byteBuf);
                        }
                        //通知requester 已经处理完毕
                        //leaderWatcher.add(ctx);
                        ByteBuf responseBuffer = getInfoWrapped(request,JoinClusterResponse);
                        ctx.writeAndFlush(responseBuffer);
                    }
                });
            }
        };
        //将回调函数传递给stateManager
        stateManager.commit(entryId,callBack);
    }

    public static void processChannelInactive(ChannelHandlerContext ctx){
        //移除在leaderWatcher中的ctx
        String info = ctxStrMap.get(ctx);
        leaderWatcher.remove(ctx);
        if (info != null && info.equals(currentLeader.address)){
            currentLeader.ctx = null;
            currentLeader.address = "null";
        }
    }

    private static ByteBuf getInfoWrapped(String info,char type){
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt(info.length()+1);
        byteBuf.writeByte(type);
        byteBuf.writeBytes(info.getBytes());
        return byteBuf;
    }


    //下面一部分,检测自己是否为Leader是的话就,开始一个Server服务.
    public static void initCheckThread(){
        NetworkIO networkIO = new NetworkIO(leaderPort);
        //checkLeaderThread.start();
    }
    public static void setLeaderPort(int leaderPort1){
        leaderPort = leaderPort1;
    }
    private static int leaderPort = -1;
    private static Thread checkLeaderThread = new Thread(new Runnable() {
        public void run() {
            boolean lastState = stateManager.isLeader();
            while(true){
                try {
                    if (lastState == false && stateManager.isLeader()){
                        /*
                        Iterator iter = leaderWatcher.iterator();
                        while(iter.hasNext()){
                            ChannelHandlerContext ctx = (ChannelHandlerContext) iter.next();
                            ctx.writeAndFlush(getInfoWrapped("leaves",LeaderChange));
                        }
                        lastState = true;
                        */
                    }
                    Thread.sleep(1000);
                    /*
                    if (stateManager.isLeader() && leaderPort != -1){
                        //默认情况下,会阻塞在这个NetworkIO的初始化方法上.所以不用担心多次重复地初始化客户端.
                        //但要是出现端口没有及时释放或者,leader角色在集群中震荡,NetworkIO的初始化会报错,这时候就让程序等一秒.
                        NetworkIO networkIO = new NetworkIO(leaderPort);
                    }
                    */
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    });
}
