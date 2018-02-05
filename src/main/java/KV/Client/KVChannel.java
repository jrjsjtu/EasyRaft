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
import java.util.concurrent.CountDownLatch;
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
    //这里的index是一致性hash之后的index


    private SocketChannel[] indexContextMap;
    CountDownLatch countDownLatch;
    EventLoopGroup group;
    private Semaphore[] semaphores = new Semaphore[2];
    public static String[] resultArray;
    KVChannel(int clusterSize) throws Exception{
        group = new NioEventLoopGroup(1);
        requestIndex = new AtomicLong(0);
        countDownLatch = new CountDownLatch(clusterSize);
        indexContextMap = new SocketChannel[clusterSize];
        semaphores = new Semaphore[2];
        for(int i=0;i<semaphores.length;i++){
            semaphores[i] = new Semaphore(0);
        }
        resultArray = new String[clusterSize];
    }

    public void waitForConnection(){
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean connectServer(final String hostName, final int port, final int shard){
        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class).
                remoteAddress(new InetSocketAddress("127.0.0.1", port)).
                handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        indexContextMap[shard] = ch;
                        countDownLatch.countDown();
                        ch.pipeline().addLast(new RequestDecoder());
                        ch.pipeline().addLast(new ResponseHandler(hostName,port,semaphores[shard],shard));
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

    private static char PutRequest = '1';
    private static char GetRequest = '2';


    public String get(String key) throws Exception{
        int payLoadSize = key.length() + 4 +1 +4;

        int idx = key.hashCode()%2;

        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt(payLoadSize).writeByte(GetRequest).writeInt(idx);
        byteBuf.writeInt(key.length()).writeBytes(key.getBytes());
        indexContextMap[idx].writeAndFlush(byteBuf);
        semaphores[idx].acquire();
        return resultArray[idx];
    }
    public void put(String key, String value) throws Exception {
        int payLoadSize = key.length() + value.length() + 8 + 4 + 1;
        ByteBuf byteBuf = Unpooled.buffer(payLoadSize+4);

        int idx = key.hashCode()%2;

        byteBuf.writeInt(payLoadSize).writeByte(PutRequest).writeInt(idx);
        byteBuf.writeInt(key.length()).writeBytes(key.getBytes());
        byteBuf.writeInt(value.length()).writeBytes(value.getBytes());

        //防止一个服务器同时被两个占用
        //System.out.println("output   " + idx);
        indexContextMap[idx].writeAndFlush(byteBuf);
        semaphores[idx].acquire();
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

