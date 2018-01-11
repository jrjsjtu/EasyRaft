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

    private static int[] kvPorts = new int[]{10200,10201};
    private static int[] raftPorts = new int[]{30303,30304};

    public void notifyClient(){
        synchronized (lock){
            lock.notify();
        }
        semp.release();
    }

    //

    public ArrayList<String> getMemberList(){
        return memberList;
    }

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
    //一旦不能和raft服务通信就停止服务!
    public void sendLog(String log) throws Exception{
        while (ch == null){}
        ByteBuf byteBuf = getByteBuffer(log,AppendLog);
        synchronized (log){
            lock = log;
            ch.writeAndFlush(byteBuf);
            log.wait();
        }
    }

    //这里api就只提供一个joinCluster.
    //返回结果:现在cluster中的成员.
    //还有注册watcher观察cluster变化的默认行为.
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
    public static void main(String[] args){
        /*
        RaftClient raftClient = new RaftClient();
        Server kvServer;
        try{
            raftClient.joinCluster("aaa");
            ArrayList<String> memberList = raftClient.getMemberList();
            String localAddress = raftClient.getLocalAddress().substring(1);
            for (int i=0;i<memberList.size();i++){
                if (memberList.get(i).equals(localAddress)){
                    kvServer = new Server(kvPorts[i]);
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        */
    }
}
