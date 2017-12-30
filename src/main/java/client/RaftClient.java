package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import state.RaftLog;

import java.net.InetSocketAddress;
import java.util.ArrayList;

/**
 * Created by jrj on 17-12-29.
 */
public class RaftClient {
    EventLoopGroup group;
    Bootstrap b;

    public static final char LeaderSelection = '0';
    public static final char RegisterWatcher = '1';
    public static final char AllocateSlot = '2';
    public static final char RegisterMember = '3';
    public static final char AppendLog = '4';
    public static final char WatcherChanged = '5';
    private static String lock;
    private static ArrayList<String> memberList;
    static{
        memberList = new ArrayList<String>();
    }
    public static void notifyClient(){
        if (lock !=null){
            synchronized (lock){
                lock.notify();
            }
        }
    }

    //
    public static void setMemberList(ArrayList<String> memberList1){
        memberList = memberList1;
    }

    public static ArrayList<String> getMemberList(){
        return memberList;
    }

    RaftClient(){
        group = new NioEventLoopGroup(1);
        b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class).
                remoteAddress(new InetSocketAddress("127.0.0.1", 30304)).
                handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new RequestDecoder());
                    }
                }).option(ChannelOption.TCP_NODELAY,false);
        try {
            ChannelFuture future = b.connect().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    //一旦不能和raft服务通信就停止服务!
    public void sendLog(String log) throws Exception{
        ByteBuf byteBuf = getByteBuffer(log,AppendLog);
        synchronized (log){
            lock = log;
            client.RequestDecoder.ctx.writeAndFlush(byteBuf);
            log.wait();
        }
    }

    //这里api就只提供一个joinCluster.
    //返回结果:现在cluster中的成员.
    //还有注册watcher观察cluster变化的默认行为.
    public void joinCluster(String clusterName) throws Exception{
        ByteBuf byteBuf = getByteBuffer(clusterName,RegisterMember);
        synchronized (clusterName){
            lock = clusterName;
            client.RequestDecoder.ctx.writeAndFlush(byteBuf);
            clusterName.wait();
        }
    }

    private ByteBuf getByteBuffer(String log,char logType){
        ByteBuf byteBuf = Unpooled.buffer(log.length() + 1 + 4);
        byteBuf.writeInt(log.length()+1);
        byteBuf.writeByte(logType);
        byteBuf.writeBytes(log.getBytes());
        return byteBuf;
    }
    public static void main(String[] args){
        RaftClient raftClient = new RaftClient();
        while (client.RequestDecoder.ctx == null){

        }
        try{
            raftClient.joinCluster("aaa");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
