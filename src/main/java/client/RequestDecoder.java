package client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;

/**
 * Created by jrj on 17-12-29.
 */
public class RequestDecoder extends ChannelInboundHandlerAdapter {
    ByteBuf header,payLoad;
    boolean readHeaderCompleted;
    int payLoadSize;
    public static ChannelHandlerContext ctx;
    public RequestDecoder(){
        readHeaderCompleted = false;
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
        //registerMember的返回结果总是
        switch (c){
            case RaftClient.WatcherChanged:
                updateArraylist(bytes);
                break;
            case RaftClient.AppendLog:
                RaftClient.notifyClient();
                break;
            case RaftClient.RegisterMember:
                updateArraylist(bytes);
                RaftClient.notifyClient();
                break;
        }
    }

    private void updateArraylist(byte[] bytes){
        ArrayList<String> strings = RaftClient.getMemberList();
        synchronized (strings){
            strings.clear();
            String[] tmps = new String(bytes).split("/");
            for (String tmp:tmps){
                System.out.println(tmp);
                strings.add(tmp);
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx1) throws Exception {
        ctx = ctx1;
    }
}
