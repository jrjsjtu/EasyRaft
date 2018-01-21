package EasyRaft.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * Created by jrj on 17-12-29.
 */
public class RequestDecoder extends ChannelInboundHandlerAdapter {
    int payLoadSize;
    String selfAddress;
    ByteBuf header,payLoad;
    RaftClient raftClient;

    ArrayList<String> aliveList;
    ArrayList<String> logList;

    boolean isLeader = false;
    public RequestDecoder(RaftClient raftClient){
        this.raftClient = raftClient;

        aliveList = new ArrayList<String>();
        logList = new ArrayList<String>();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        if (header==null){
            header = Unpooled.buffer(4);
        }
        if (header.writableBytes()>0){
            buf.readBytes(header);
            //这里是当writeable>0 的情况下writeable = 0,一次request有且仅有一次发生
            if (header.writableBytes()==0){
                payLoadSize = header.readInt();
                if (payLoadSize<0){

                }else{
                    payLoad = Unpooled.buffer(payLoadSize);
                }
            }
        }
        if (payLoad != null && payLoad.writableBytes()>0){
            buf.readBytes(payLoad);
            if (payLoad.writableBytes() == 0){
                processPayLoad(payLoad);
                payLoad.release();
                header.resetWriterIndex();
                header.resetReaderIndex();
            }
        }
        buf.release();
    }

    private void processPayLoad(ByteBuf payLoad){
        char c = (char) payLoad.readByte();
        byte[] bytes = new byte[payLoad.readableBytes()];
        payLoad.readBytes(bytes);
        System.out.println(new String(bytes));
        //registerMember的返回结果总是
        switch (c){
            case RaftClient.RegisterWatcher:
                initTwoList(bytes);
                break;
            case RaftClient.ServerJoinCommited:
                addMember(bytes);
                break;
            case RaftClient.ServerLeaveCommited:
                deleteMember(bytes);
                break;
            case RaftClient.LeaveCluster:
                leaveMember(bytes);
                break;
            case RaftClient.ClientWakeUp:
                raftClient.notifyClient();
                break;
        }
    }

    private void leaveMember(byte[] bytes){
        String tmp = new String(bytes).substring(7);
        aliveList.remove(tmp);
        checkLeaderAndCommit();
    }

    private void checkLeaderAndCommit(){
        ArrayList<String> backUp = raftClient.getMemberList();
        final StringBuilder stringBuilder = new StringBuilder();
        for (String tmp:backUp){
            if (tmp.equals(selfAddress)){
                isLeader = true;
                break;
            }
            if (aliveList.contains(tmp)){
                isLeader = false;
                break;
            }else{
                stringBuilder.append('/');
                stringBuilder.append(tmp);
            }
        }

        if (isLeader){
            stringBuilder.deleteCharAt(0);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        raftClient.getSemaphore();
                        ByteBuf byteBuf = Unpooled.buffer();
                        byteBuf.writeInt(stringBuilder.length()+1);
                        byteBuf.writeChar(RaftClient.CommitLeaveLog);
                        byteBuf.writeBytes(stringBuilder.toString().getBytes());
                        ctx.writeAndFlush(byteBuf);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void deleteMember(byte[] bytes){
        String tmp = new String(bytes).substring(7);
        logList.remove(tmp);
        aliveList.remove(tmp);
    }

    private void initTwoList(byte[] bytes){
        String twoList = new String(bytes);
        System.out.println(twoList);
        String[] lists = twoList.split("\\?");
        String[] logs = lists[0].split("\\|");

        ArrayList<String> strings = raftClient.getMemberList();
        for (String tmp:logs){
            if (tmp.charAt(0)=='a'){
                String address = tmp.substring(5);
                strings.add(address);
            }else if(tmp.charAt(0)=='l'){
                String address = tmp.substring(7);
                strings.remove(address);
            }
        }

        String[] lives = lists[1].split("/");
        for(String tmp:lives){
            aliveList.add(tmp);
        }
        checkLeaderAndCommit();
    }

    private void addMember(byte[] bytes){
        String tmp = new String(bytes).substring(5);
        aliveList.add(tmp);
        logList.add(tmp);
    }

    ChannelHandlerContext ctx;
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        selfAddress = ctx.pipeline().channel().localAddress().toString().substring(1);
        this.ctx = ctx;
    }
}
