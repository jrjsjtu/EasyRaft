package EasyRaft.client;

import KV.KVDatabase.Server;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

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


    private String lock;
    private ArrayList<String> memberList;
    final Semaphore semp = new Semaphore(1);

    void notifyClient(){
        synchronized (lock){
            lock.notify();
        }
        semp.release();
    }

    //


    SocketChannel ch;
    public RaftClient(){
        //lock用来实现request的response收到后的提醒
        //Semaphore 来保证同一时间只能有一个request on the fly主要是为了程序逻辑的实现方便.
        lock = "lock";
        memberList = new ArrayList<String>();

        group = new NioEventLoopGroup(1);
        b = new Bootstrap();
        final RaftClient tmp =this;
        b.group(group).channel(NioSocketChannel.class).
                remoteAddress(new InetSocketAddress("127.0.0.1", 30303)).
                handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        tmp.ch = ch;
                        ch.pipeline().addLast(new RequestDecoder(tmp));
                    }
                }).option(ChannelOption.TCP_NODELAY,false);
        try {
            ChannelFuture future = b.connect().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getMemberList(){
        return memberList;
    }

    /**
     * The api for join the cluster naming @clusterName,
     * this api will bring 2 extra effect
     * 1.the change of the cluster will be notified,then onMemberFails will be called.This implies the registerWatcher is automatically called.
     * 2.once join successfully, the alive ip:port in the cluster will be passed to onJoinCluster as a parameter, the logs will be another parameter.
     *
     * @param clusterName the name of the cluster the caller want to join, it is designed for server which want to provide service with the help of raft
     * @param onJoinCluster the callBack Function when successFully join the Cluster.
     * @param onMemberFails the callBack Function when a member in the cluster down.
     */
    public void joinCluster(String clusterName) throws Exception{
        while (ch == null){}
        semp.acquire();
        ByteBuf byteBuf = getByteBuffer(clusterName,RegisterMember);
        synchronized (clusterName){
            lock = clusterName;
            ch.writeAndFlush(byteBuf);
            clusterName.wait();
        }
    }

    /**
     * The api for watch the change of @clusterName,
     *
     * @param clusterName current term of the caller of the rpc
     */
    public void registerWatcher(String clusterName) throws Exception{
        while (ch == null){}
        semp.acquire();
        ByteBuf byteBuf = getByteBuffer(clusterName,RegisterWatcher);
        synchronized (clusterName){
            lock = clusterName;
            ch.writeAndFlush(byteBuf);
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

    public String getLocalAddress(){
        return ch.localAddress().toString();
    }
}
