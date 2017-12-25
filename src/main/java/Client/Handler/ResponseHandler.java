package Client.Handler;

import Client.KVChannel;
import KVDatabase.KVHashMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by jrj on 17-12-25.
 */
public class ResponseHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        long ackIndex = buf.readLong();
        //System.out.println(ackIndex);
        KVChannel.awaitClient(ackIndex);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        KVChannel.setChannelHandlerContext(ctx);
    }
}
