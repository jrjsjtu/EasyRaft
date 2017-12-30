package KV.Client.Handler;

import KV.Client.KVChannel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.HashMap;

/**
 * Created by jrj on 17-12-25.
 */
public class ResponseHandler extends ChannelInboundHandlerAdapter {
    String hostName;
    int port;
    private static HashMap<String,ChannelHandlerContext> hostContextMap;
    static {
        hostContextMap = new HashMap<String, ChannelHandlerContext>();
    }
    public ResponseHandler(){

    }

    public ResponseHandler(String hostName,int port){
        this.hostName = hostName;
        this.port = port;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        long ackIndex = buf.readLong();
        buf.release();
        //System.out.println(ackIndex);
        KVChannel.awaitClient(ackIndex);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        KVChannel.setChannelHandlerContext(ctx);
        if (hostName != null){
            hostContextMap.put(hostName + ":"+ port,ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (hostName != null && hostContextMap.containsKey(hostName)){
            hostContextMap.remove(hostName);
        }
    }
}
