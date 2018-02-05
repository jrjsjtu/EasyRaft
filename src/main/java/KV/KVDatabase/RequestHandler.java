package KV.KVDatabase;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by jrj on 17-12-24.
 */
public class RequestHandler extends ChannelInboundHandlerAdapter {
    private final static char PutRequest = '1';
    private final static char GetRequest = '2';
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        char type = (char)buf.readByte();

        int index = buf.readInt();
        int keySize = buf.readInt();
        byte[] keyBytes = new byte[keySize];
        buf.readBytes(keyBytes);
        String key = new String(keyBytes);

        switch (type){
            case PutRequest:
                int valueSize = buf.readInt();
                byte[] valueBytes = new byte[valueSize];
                buf.readBytes(valueBytes);

                KVHashMap.hashMap.put(key,new String(valueBytes));
                ByteBuf byteBuf = Unpooled.buffer(8);
                byteBuf.writeInt(4);
                byteBuf.writeInt(-1);
                ctx.writeAndFlush(byteBuf);
                break;
            case GetRequest:
                String result = KVHashMap.hashMap.get(key);
                ByteBuf resultBuf = Unpooled.buffer();
                if (result==null){
                    resultBuf.writeInt(4);
                    resultBuf.writeInt(0);
                }else{
                    resultBuf.writeInt(4+result.length());
                    resultBuf.writeInt(result.length());
                    resultBuf.writeBytes(result.getBytes());
                }
                ctx.writeAndFlush(resultBuf);
                break;
        }
    }
}
