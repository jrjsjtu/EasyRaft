package KV.Client.Handler;

import KV.Client.KVChannel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

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

    Semaphore semaphore;
    int shard;
    public ResponseHandler(String hostName, int port, Semaphore semaphore,int shard){
        this.hostName = hostName;
        this.port = port;
        this.semaphore = semaphore;
        this.shard = shard;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        int ackIndex = buf.readInt();
        if(ackIndex==-1){

        }else if(ackIndex==0){
            KVChannel.resultArray[shard] = null;
        }else{
            byte[] bytes = new byte[ackIndex];
            buf.readBytes(bytes);
            KVChannel.resultArray[shard] = new String(bytes);
            //System.out.println(result);
        }
        buf.release();
        //KVChannel.awaitClient(ackIndex);
        semaphore.release();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
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
