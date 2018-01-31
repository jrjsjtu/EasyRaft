package EasyRaft.ServerIO.handler;

import EasyRaft.ServerIO.RaftKeeper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


/**
 * Created by jrj on 17-12-24.
 */
public class RequestDecoder extends ChannelInboundHandlerAdapter {
    ByteBuf header,payLoad;
    int payLoadSize;

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
                    ByteBuf byteBufError =  Unpooled.buffer();
                    byteBufError.writeBytes("Error".getBytes());
                    ctx.writeAndFlush(byteBufError);
                    buf.release();
                    return;
                }else{
                    payLoad = Unpooled.buffer(payLoadSize);
                }
            }
        }
        if (payLoad != null && payLoad.writableBytes()>0){
            buf.readBytes(payLoad);
            if (payLoad.writableBytes() == 0){
                RaftKeeper.processRequest(ctx,payLoad);
                header.resetWriterIndex();
                header.resetReaderIndex();
            }
        }
        buf.release();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        RaftKeeper.processChannelInactive(ctx);
    }
}
