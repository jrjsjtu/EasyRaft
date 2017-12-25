package Client;

import Client.Handler.ResponseHandler;
import KVDatabase.RequestDecoder;
import KVDatabase.RequestHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 * Created by jrj on 17-12-25.
 */
public class KVChannel implements KVProtocol{

    //这里的index是客户端分配的每一条消息的id,ack返回之后根据ack中的long找到线程然后唤醒
    //这边的String其实起到的就是lock的作用,然客户线程wait在lock上,然后在netty线程中用await唤醒
    private static ChannelHandlerContext channelHandlerContext;
    private static HashMap<Long,String> indexThreadMap;
    //这里的index是一致性hash之后的index
    private static HashMap<Integer,ChannelHandlerContext> indexContextMap;

    static{
        indexThreadMap = new HashMap<Long, String>();
        indexContextMap = new HashMap<Integer, ChannelHandlerContext>();
    }
    EventLoopGroup group;
    KVChannel() throws Exception{
        Bootstrap b = new Bootstrap();
        group = new NioEventLoopGroup(1);
        b.group(group).channel(NioSocketChannel.class).
                remoteAddress(new InetSocketAddress("127.0.0.1", 30303)).
                handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new RequestDecoder());
                        ch.pipeline().addLast(new ResponseHandler());
                    }
                });
        b.connect().sync();
    }

    public static void awaitClient(long index){
        String key;
        synchronized (indexThreadMap){
            key = indexThreadMap.get(index);
        }
        synchronized (key){
            key.notify();
        }
    }

    public static void setChannelHandlerContext(ChannelHandlerContext ctx){
        channelHandlerContext = ctx;
    }

    public void put(long requestIndex, String key, String value) throws Exception {
        int payLoadSize = key.length() + value.length() + 8 + 8;
        ByteBuf byteBuf = Unpooled.buffer(payLoadSize+4);
        byteBuf.writeInt(payLoadSize).writeLong(requestIndex);
        byteBuf.writeInt(key.length()).writeBytes(key.getBytes());
        byteBuf.writeInt(value.length()).writeBytes(value.getBytes());
        synchronized (channelHandlerContext){
            indexThreadMap.put(requestIndex,key);
        }
        synchronized (key){
            channelHandlerContext.writeAndFlush(byteBuf);
            key.wait();
        }
    }
}
