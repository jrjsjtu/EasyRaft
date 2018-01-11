package KV.Client;

import KV.Client.Handler.ResponseHandler;
import KV.KVDatabase.RequestDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by jrj on 17-12-25.
 */
public class KVChannel implements KVProtocol{
    private AtomicLong requestIndex;
    //这里的index是客户端分配的每一条消息的id,ack返回之后根据ack中的long找到线程然后唤醒
    //这边的String其实起到的就是lock的作用,然客户线程wait在lock上,然后在netty线程中用await唤醒
    private static ChannelHandlerContext channelHandlerContext;
    private static HashMap<Long,String> indexThreadMap;
    //这里的index是一致性hash之后的index
    private static SocketChannel[] indexContextMap;
    private static Semaphore[] semaphores;
    private static int[] kvPorts = new int[]{10200,10201};
    public static String lock;
    static{
        indexThreadMap = new HashMap<Long, String>();
        indexContextMap = new SocketChannel[2];
        semaphores = new Semaphore[2];
        for (int i=0;i<2;i++){
            semaphores[i] = new Semaphore(1);
        }
    }
    EventLoopGroup group;
    KVChannel() throws Exception{
        group = new NioEventLoopGroup(1);
        requestIndex = new AtomicLong(0);
        /*
        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class).
                remoteAddress(new InetSocketAddress("127.0.0.1", 30303)).
                handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new RequestDecoder());
                        ch.pipeline().addLast(new ResponseHandler());
                    }
                });
        b.connect().sync();
        */
    }

    private static ArrayList<String> metaInfoMap;
    static {
        metaInfoMap = new ArrayList<String>();
    }
    //这里是要问leader要数据的,因为有发生脑裂的情况,唯一知道集群中server数量的只有leader了
    public void getLeadrInfo(){

    }

    public void waitForConnection() throws Exception{
        semaphore.acquire();
    }
    Semaphore semaphore = new Semaphore(-1);
    public boolean connectServer(final String hostName, final int port){
        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class).
                remoteAddress(new InetSocketAddress("127.0.0.1", port)).
                handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        indexContextMap[port-10200] = ch;
                        semaphore.release();
                        ch.pipeline().addLast(new RequestDecoder());
                        ch.pipeline().addLast(new ResponseHandler(hostName,port));
                    }
                });
        try{
            b.connect().sync();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void awaitClient(int index){
        /*
        String key;
        synchronized (indexThreadMap){
            key = indexThreadMap.get(index);
            indexThreadMap.remove(index);
        }
        synchronized (key){
            key.notify();
        }
        */
        semaphores[index].release();
        /*
        synchronized (lock){
            lock.notify();
        }
        */
    }

    public static void setChannelHandlerContext(ChannelHandlerContext ctx){
        channelHandlerContext = ctx;
    }


    public void put(long requestIndex, String key, String value) throws Exception {
        int payLoadSize = key.length() + value.length() + 8 + 4;
        ByteBuf byteBuf = Unpooled.buffer(payLoadSize+4);

        int idx = key.hashCode()%2;

        byteBuf.writeInt(payLoadSize).writeInt(idx);
        byteBuf.writeInt(key.length()).writeBytes(key.getBytes());
        byteBuf.writeInt(value.length()).writeBytes(value.getBytes());

        //防止一个服务器同时被两个占用
        //System.out.println("output   " + idx);
        semaphores[idx].acquire();
        indexContextMap[idx].writeAndFlush(byteBuf);
        /*
        synchronized (key){
            indexContextMap[idx].writeAndFlush(byteBuf);
            lock = key;
            key.wait();
        }
        */
        //channelHandlerContext.writeAndFlush(byteBuf);
    }

    static class SpinLock {
        //java中原子（CAS）操作
        AtomicInteger integer = new AtomicInteger(2);
        public void lock() {
            //lock函数将owner设置为当前线程，并且预测原来的值为空。unlock函数将owner设置为null，并且预测值为当前线程。当有第二个线程调用lock操作时由于owner值不为空，导致循环
            //一直被执行，直至第一个线程调用unlock函数将owner设置为null，第二个线程才能进入临界区。
            integer.compareAndSet(2,1);
            while (!integer.compareAndSet(0, 2)){
            }
        }
        public void unLock() {
            if(integer.compareAndSet(1,0)){
                System.out.println("awake");
            }
        }
    }
}

