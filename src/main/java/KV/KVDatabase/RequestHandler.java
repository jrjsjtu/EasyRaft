package KV.KVDatabase;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by jrj on 17-12-24.
 */
public class RequestHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        int index = buf.readInt();
        int keySize = buf.readInt();
        byte[] keyBytes = new byte[keySize];
        buf.readBytes(keyBytes);

        int valueSize = buf.readInt();
        byte[] valueBytes = new byte[valueSize];
        buf.readBytes(valueBytes);

        KVHashMap.hashMap.put(new String(keyBytes),new String(valueBytes));
        ByteBuf byteBuf = Unpooled.buffer(8);
        byteBuf.writeInt(4);
        byteBuf.writeInt(index);

        //System.out.println("receive " + new String(keyBytes)  + "  " + new String(valueBytes));
        ctx.writeAndFlush(byteBuf);
    }
}
