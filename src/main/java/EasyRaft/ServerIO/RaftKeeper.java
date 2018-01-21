package EasyRaft.ServerIO;

import EasyRaft.StateManager;
import EasyRaft.state.State;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import sun.nio.ch.Net;

import java.util.ArrayList;

/**
 * Created by jrj on 17-12-28.
 */
//这一层keeper层构建在Raft之上,通过raft的接口运作
public class RaftKeeper {
    private static StateManager stateManager;

    public static void setStateManager(StateManager stateManager0){
        stateManager = stateManager0;
    }
    public static final char RegisterWatcher = '1';
    public static final char RegisterMember = '3';
    public static final char CommitLeaveLog = '6';
    public static final char ClientWakeUp = '7';

    public static final char LeaveCluster = '5';
    public static final char ServerJoinCommited = '8';
    public static final char ServerLeaveCommited = '9';

    private static ArrayList<ChannelHandlerContext> memberList;
    private static ArrayList<ChannelHandlerContext> wathcerList;
    private static ArrayList<String> logsList;

    static{
        memberList = new ArrayList<ChannelHandlerContext>();
        wathcerList = new ArrayList<ChannelHandlerContext>();
        logsList = new ArrayList<String>();
    }

    public static String processRequest(ChannelHandlerContext ctx,ByteBuf byteBuf){
        char c = (char) byteBuf.readByte();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        switch (c){
            case RegisterWatcher:
                processRegisterWatcher(ctx,new String(bytes));
                break;
            case RegisterMember:
                processRegisterMember(ctx);
                break;
            case CommitLeaveLog:
                processCommitLeaveLog(ctx,new String(bytes));
                break;
        }
        return null;
    }

    public static void processChannelInactive(char c,ChannelHandlerContext ctx){
        String address = ctx.channel().remoteAddress().toString();
        switch (c){
            case LeaveCluster:
                if(wathcerList.contains(ctx)){
                    wathcerList.remove(ctx);
                }
                if(memberList.contains(ctx)){
                    memberList.remove(ctx);
                }
                sendLeaveMessage(address);
                break;
        }
    }

    private static void sendLeaveMessage(String message){
        ByteBuf byteBuf = getInfoWrapped(message,LeaveCluster);

        //通知所有的watcher有一个member离线了,但是并不是提交了log而引起的变化
        for(ChannelHandlerContext tmpCtx:memberList){
            byteBuf.retain();
            tmpCtx.writeAndFlush(byteBuf);
        }
        byteBuf.release();
    }

    private static void processRegisterWatcher(ChannelHandlerContext ctx,String entry){
        wathcerList.add(ctx);
        String info = getCurrentCommitedInfo();
        ctx.writeAndFlush(getInfoWrapped(info,RegisterWatcher));
        ctx.writeAndFlush(getInfoWrapped("wake",ClientWakeUp));
    }

    private static String getCurrentCommitedInfo(){
        // add:/127.0.0.1:55746|add:/127.0.0.1:55752?127.0.0.1:55746/127.0.0.1:55752
        StringBuilder stringBuilder = new StringBuilder();

        for (ChannelHandlerContext tmp:memberList){
            stringBuilder.append('|');
            stringBuilder.append(tmp.pipeline().channel().remoteAddress().toString());
        }
        stringBuilder.deleteCharAt(0);
        stringBuilder.append('?');

        for (String tmp:logsList){
            stringBuilder.append(tmp);
        }

        return stringBuilder.toString();
    }
    /*
    //这个函数需要放在raft线程中运行,不然getAllLogs()会引起race condition
    //之后可以添加关于thread的检验
    private static String getLogsTillNow0(){
        StringBuilder stringBuilder = new StringBuilder();
        //把现在存活的ctx也放到报文中,方便与log对照
        //用这个方式得到ip:port的总是有一个/在最前面很烦.
        for (ChannelHandlerContext tmp:memberList){
            stringBuilder.append(tmp.pipeline().channel().remoteAddress().toString());
        }
        stringBuilder.deleteCharAt(0);
        //问号用来分割存活ctx以及logs
        stringBuilder.append('?');
        //放入所有的log
        stringBuilder.append();
        String result = stringBuilder.toString();
        return result;
    }

    private  static ByteBuf getLogsTillNow(char type){
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt(result.length()+1);
        byteBuf.writeByte(type);
        byteBuf.writeBytes(result.getBytes());
        return byteBuf;
    }
    */

    private static ByteBuf getInfoWrapped(String info,char type){
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(info.length()+1);
        byteBuf.writeChar(type);
        byteBuf.writeBytes(info.getBytes());
        return byteBuf;
    }

    private static void processRegisterMember(final ChannelHandlerContext ctx){
        final String entryId = "add:" + ctx.pipeline().channel().remoteAddress().toString();
        //这个runnable只有在成功commit后才会调用,重新将任务放到netty的进程中,防止race condition!
        Runnable callBack = new Runnable() {
            public void run() {
                //老实说,像这样嵌套的回调真的挺难受的.
                ctx.executor().submit(new Runnable() {
                    public void run() {
                        //一旦registerMember,那么就默认registerWatcher了.因此这边向两个list中同时加入ctx
                        wathcerList.add(ctx);
                        memberList.add(ctx);
                        logsList.add(entryId);
                        ByteBuf byteBuf = getInfoWrapped(entryId,ServerJoinCommited);

                        for(ChannelHandlerContext watcher:wathcerList){
                            byteBuf.retain();
                            watcher.writeAndFlush(byteBuf);
                        }
                        byteBuf.release();
                        //告诉请求的调用者可以醒来,并发起新的请求了
                        ctx.writeAndFlush(getInfoWrapped("wake",ClientWakeUp));
                    }
                });
            }
        };
        //将回调函数传递给stateManager
        stateManager.commit(entryId,callBack);
    }

    private static void processCommitLeaveLog(final ChannelHandlerContext ctx, final String address){
        final String entryId = "leave:" + address;
        //这个runnable只有在成功commit后才会调用,并且将在Raft的业务线程中运行
        Runnable callBack = new Runnable() {
            //老实说,像这样嵌套的回调真的挺难受的.
            public void run() {
                ctx.executor().submit(new Runnable() {
                    public void run() {
                        logsList.add(address);
                        ByteBuf byteBuf = getInfoWrapped(entryId,ServerLeaveCommited);
                        for(ChannelHandlerContext watcher:wathcerList){
                            byteBuf.retain();
                            watcher.writeAndFlush(byteBuf);
                        }
                        byteBuf.release();
                        //告诉请求的调用者可以醒来,并发起新的请求了
                        ctx.writeAndFlush(getInfoWrapped("wake",ClientWakeUp));
                    }
                });
            }
        };
        //将回调函数传递给stateManager
        stateManager.commit(entryId,callBack);
    }

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
