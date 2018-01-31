package EasyRaft.ServerIO;

import EasyRaft.StateManager;
import EasyRaft.requests.MemberUpRequest;
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


    public static String processRequest(ChannelHandlerContext ctx,ByteBuf byteBuf){
        char c = (char) byteBuf.readByte();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        switch (c){
            case SelectLeaderRequest:
                processSelectLeader(ctx,new String(bytes));
                break;
            case SetSlotRequest:
                processSetSlot(ctx,new String(bytes));
                break;
            case JoinClusterRequest:
                processJoinCluster(ctx,new String(bytes));
                break;
            case LeaveClusterRequest:
                processLeaveCluster(ctx,new String(bytes));
                break;
            case QueryAliveRequest:
                processAliveQuery(ctx,new String(bytes));
                break;
            case QuerySlotRequest:
                processSlotQuery(ctx,new String(bytes));
                break;
        }
        return null;
    }

    private static CurrentLeader currentLeader = CurrentLeader.nullLeader;
    private static HashSet<ChannelHandlerContext> leaderWatcher = new HashSet<ChannelHandlerContext>();
    private static ArrayList<String> slot = new ArrayList<String>();
    static Hashtable<ChannelHandlerContext,String> ctxStrMap = new Hashtable<ChannelHandlerContext, String>();

    private static void processLeaveCluster(final ChannelHandlerContext ctx,final String request){
        final String entryId = "leaveCluster:" + request;
        Runnable callBack = new Runnable() {
            public void run() {
                String[] info = request.split("\\|");
                for(Map.Entry<ChannelHandlerContext,String> entry:ctxStrMap.entrySet()){
                    if (entry.getValue().equals(info[1])){
                        ctxStrMap.remove(entry.getKey());
                        ByteBuf byteBuf = getInfoWrapped(request,LeaveClusterResponse);
                        ctx.writeAndFlush(byteBuf);
                        break;
                    }
                }
            }
        };
        //将回调函数传递给stateManager
        stateManager.commit(entryId,callBack);
    }

    private static void processAliveQuery(final ChannelHandlerContext ctx,final String request){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(request);
        for(ChannelHandlerContext tmp:leaderWatcher){
            stringBuilder.append('|').append(ctxStrMap.get(tmp));
        }
        ByteBuf byteBuf = getInfoWrapped(stringBuilder.toString(),QueryAliveResponse);
        ctx.writeAndFlush(byteBuf);
    }

    private static void processSlotQuery(final ChannelHandlerContext ctx,final String request){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(request);
        for(String tmp:slot){
            stringBuilder.append('|').append(tmp);
        }
        ByteBuf byteBuf = getInfoWrapped(stringBuilder.toString(),QuerySlotResponse);
        ctx.writeAndFlush(byteBuf);
    }

    private static void processSelectLeader(final ChannelHandlerContext ctx,final String request){
        final String entryId = "leaderSelect:" + request;
        //这个runnable只有在成功commit后才会调用,重新将任务放到netty的进程中,防止race condition!
        Runnable callBack = new Runnable() {
            public void run() {
                //老实说,像这样嵌套的回调真的挺难受的.
                ctx.executor().submit(new Runnable() {
                    public void run() {
                        String[] info = request.split("\\|");
                        int epoch = Integer.parseInt(info[1]);
                        if (epoch>currentLeader.epoch){
                            currentLeader = new CurrentLeader(epoch,ctx.channel().remoteAddress().toString().substring(1),ctx);
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
                //老实说,像这样嵌套的回调真的挺难受的.
                ctx.executor().submit(new Runnable() {
                    public void run() {
                        String[] info = request.split("\\|");
                        ctxStrMap.put(ctx,info[1]);
                        if (currentLeader.ctx!= null){
                            //通知leader有人上线
                            ByteBuf byteBuf =getInfoWrapped(info[1], NotifyMemberUp);
                            currentLeader.ctx.writeAndFlush(byteBuf);
                        }
                        //通知requester 已经处理完毕
                        leaderWatcher.add(ctx);
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
        if (ctx.equals(currentLeader.ctx)){
            leaderWatcher.remove(ctx);

            currentLeader.ctx = null;
            currentLeader.address = "null";

            ByteBuf byteBuf = getInfoWrapped(currentLeader.epoch+"",NotifyLeaderDown);
            for(ChannelHandlerContext watcher:leaderWatcher){
                byteBuf.retain();
                watcher.writeAndFlush(byteBuf);
            }
            byteBuf.release();
        }else if(leaderWatcher.contains(ctx)){
            leaderWatcher.remove(ctx);
            String info = ctxStrMap.get(ctx);
            ByteBuf byteBuf = getInfoWrapped(info,NotifyMemberDown);
            currentLeader.ctx.writeAndFlush(byteBuf);
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
        checkLeaderThread.start();
    }
    public static void setLeaderPort(int leaderPort1){
        leaderPort = leaderPort1;
    }
    private static int leaderPort = -1;
    private static Thread checkLeaderThread = new Thread(new Runnable() {
        public void run() {
            while(true){
                try {
                    if (stateManager.isLeader() && leaderPort != -1){
                        //默认情况下,会阻塞在这个NetworkIO的初始化方法上.所以不用担心多次重复地初始化客户端.
                        //但要是出现端口没有及时释放或者,leader角色在集群中震荡,NetworkIO的初始化会报错,这时候就让程序等一秒.
                        NetworkIO networkIO = new NetworkIO(leaderPort);
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    },"checkLeaderThread");
}
