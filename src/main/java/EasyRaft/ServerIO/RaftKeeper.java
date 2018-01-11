package EasyRaft.ServerIO;

import EasyRaft.StateManager;
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
    public static final char LeaderSelection = '0';
    public static final char RegisterWatcher = '1';
    public static final char AllocateSlot = '2';
    public static final char RegisterMember = '3';
    public static final char AppendLog = '4';
    public static final char LeaveCluster = '5';

    private static ArrayList<String> memberList;
    private static ArrayList<ChannelHandlerContext> wathcerList;
    static{
        memberList = new ArrayList<String>();
        wathcerList = new ArrayList<ChannelHandlerContext>();
    }

    public static String processRequest(ChannelHandlerContext ctx,ByteBuf byteBuf){
        char c = (char) byteBuf.readByte();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        switch (c){
            case AppendLog:
                processLeaderInsertLog(ctx,new String(bytes));
                break;
            case LeaderSelection:
                break;
            case RegisterWatcher:
                processRegisterWatcher(ctx,new String(bytes));
                break;
            case AllocateSlot:
                break;
            case RegisterMember:
                processRegisterMember(ctx);
                break;
        }
        return null;
    }

    public static String processRequest(char c,ChannelHandlerContext ctx){
        String address = ctx.channel().remoteAddress().toString();
        switch (c){
            case LeaveCluster:
                /*
                if(memberList.contains(address)){
                    memberList.remove(address);
                    System.out.println("remove memberList " + address);
                }
                */
                if(wathcerList.contains(ctx)){
                    wathcerList.remove(ctx);
                    System.out.println("remove ctx " + address);
                }
                break;
        }
        return null;
    }

    private static void processRegisterWatcher(ChannelHandlerContext ctx,String entry){
        synchronized (wathcerList){
            wathcerList.add(ctx);
            ByteBuf byteBuf = Unpooled.buffer();
            StringBuilder stringBuilder = new StringBuilder();
            for (String tmp:memberList){
                stringBuilder.append(tmp);
            }
            stringBuilder.deleteCharAt(0);
            String result = stringBuilder.toString();

            byteBuf.writeInt(result.length()+1);
            byteBuf.writeByte(RegisterWatcher);
            byteBuf.writeBytes(result.getBytes());

            ctx.writeAndFlush(byteBuf);
        }
    }

    private static void processRegisterMember(final ChannelHandlerContext ctx){
        String entryId = "add:" + ctx.pipeline().channel().remoteAddress().toString();
        Runnable callBack = new Runnable() {
            public void run() {
                synchronized (wathcerList){
                    wathcerList.add(ctx);
                    memberList.add(ctx.pipeline().channel().remoteAddress().toString());
                    ByteBuf byteBuf = Unpooled.buffer();
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String tmp:memberList){
                        stringBuilder.append(tmp);
                    }
                    stringBuilder.deleteCharAt(0);
                    String result = stringBuilder.toString();
                    System.out.println(result);
                    byteBuf.writeInt(result.length()+1);
                    byteBuf.writeByte(RegisterMember);
                    byteBuf.writeBytes(result.getBytes());
                    for(ChannelHandlerContext watcher:wathcerList){
                        byteBuf.retain();
                        watcher.writeAndFlush(byteBuf);
                    }
                    byteBuf.release();
                }
            }
        };
        stateManager.commit(entryId,callBack);
    }

    private static void processLeaderInsertLog(ChannelHandlerContext ctx,String entry){
        stateManager.commit(entry,ctx);
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
