package ServerIO;

import EasyRaft.StateManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by jrj on 17-12-28.
 */
//这一层keeper层构建在Raft之上,通过raft的接口运作
public class RaftKeeper {
    private static StateManager stateManager;


    public static void setStateManager(StateManager stateManager0){
        stateManager = stateManager0;
    }
    private static final char LeaderSelection = '0';
    private static final char RegisterWatcher = '1';
    private static final char AllocateSlot = '2';
    private static final char RegisterMember = '3';
    private static final char AppendLog = '4';

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

    public static void processRegisterWatcher(ChannelHandlerContext ctx,String entry){
        stateManager.commit(entry,ctx);
    }

    public static void processRegisterMember(final ChannelHandlerContext ctx){
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

    public static void processLeaderInsertLog(ChannelHandlerContext ctx,String entry){
        stateManager.commit(entry,ctx);
    }

    public ByteBuf processRegisterWatcher(ChannelHandlerContext ctx){
        return null;
    }

    public ByteBuf processAllocateSlot(ChannelHandlerContext ctx){
        return null;
    }


    public static void main(String[] args){
        try {
            StateManager stateManager = new StateManager();
            RaftKeeper.setStateManager(stateManager);
            //raftKeeper.processLeaderSelection("test log");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
