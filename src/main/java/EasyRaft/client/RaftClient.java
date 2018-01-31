package EasyRaft.client;

import EasyRaft.client.callBack.RaftCallBack;
import EasyRaft.client.callBack.RaftClientImp;
import EasyRaft.requests.*;
import KV.KVDatabase.KVServerCallBack;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jrj on 17-12-29.
 */
public class RaftClient implements RaftClientImp{
    EventLoopGroup group;
    Bootstrap b;

    public static final char SelectLeaderRequest = '1';
    public static final char SelectLeaderResponse = '5';

    public static final char NotifyLeaderDown = '4';

    public static final char NotifyMemberDown = '7';
    public static final char NotifyMemberUp = '8';

    public static final char SetSlotRequest = '2';
    public static final char SetSlotResponse= '6';

    public static final char QuerySlotRequest = 'a';
    public static final char QuerySlotResponse = 'b';

    public static final char QueryAliveRequest = 'c';
    public static final char QueryAliveResponse = 'd';

    public static final char JoinClusterRequest = 'e';
    public static final char JoinClusterResponse = 'f';

    public static final char LeaveClusterRequest = 'g';
    public static final char LeaveClusterResponse = 'h';


    AtomicInteger requestOrder = new AtomicInteger(0);

    private SocketChannel ch;
    private int epoch = 0;

    private String selfAddress;

    public String getSelfAddress() {
        return selfAddress;
    }

    public void setSelfAddress(String selfAddress) {
        this.selfAddress = selfAddress;
    }

    public int getEpoch() {
        return epoch;
    }

    public void setEpoch(int epoch) {
        this.epoch = epoch;
    }

    LinkedBlockingQueue<RaftRequest> callBackTask = new LinkedBlockingQueue<RaftRequest>();
    Thread callBackThread = new Thread(new Runnable() {
        public void run() {
            try {
                while(true){
                    RaftRequest tmp = callBackTask.take();
                    if (tmp instanceof SelectLeaderRequest){
                        SelectLeaderRequest selectLeaderRequest = (SelectLeaderRequest)tmp;
                        raftCallBack.onBecomeLeader(localRaftClient);
                        selectLeaderRequest.notifyResponse();
                    }else if(tmp instanceof LeaderDownRequest){
                        LeaderDownRequest leaderDownRequest = (LeaderDownRequest)tmp;
                        int epoch = leaderDownRequest.getEpoch() + 1;
                        setEpoch(epoch);
                        localRaftClient.electLeader(epoch);
                    }else if(tmp instanceof MemberUpRequest){
                        MemberUpRequest memberUpRequest = (MemberUpRequest)tmp;
                        raftCallBack.onMemberJoinWhenLeader(memberUpRequest.getIdx(),memberUpRequest.getAddress());
                    }else if(tmp instanceof MemberDownRequest){
                        System.out.println("member down");
                        MemberDownRequest memberDownRequest = (MemberDownRequest)tmp;
                        raftCallBack.onMemberLeaveWhenLeader(memberDownRequest.getAddress());
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    },"RaftCallBack-Thread");

    RaftCallBack raftCallBack;
    RaftClient localRaftClient;

    public RaftClient(RaftCallBack raftCallBack){
        this.raftCallBack = raftCallBack;
        this.localRaftClient = this;
    }

    public RaftClient(){
        this.raftCallBack = new DefaultCallBack();
        this.localRaftClient = this;
    }

    private final class DefaultCallBack implements RaftCallBack{
        public void onBecomeLeader(RaftClientImp raftClientImp) {

        }

        public void onLeaderFailed(int epoch) {

        }

        public void onMemberJoinWhenLeader(int idx, String address) {

        }

        public void onMemberLeaveWhenLeader(String address) {

        }
    }
    public void joinRaft(){
        group = new NioEventLoopGroup(1);
        b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class).
                remoteAddress(new InetSocketAddress("127.0.0.1", 30303)).
                handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        localRaftClient.ch = ch;
                        ch.pipeline().addLast(new RequestDecoder(localRaftClient,raftCallBack));
                    }
                }).option(ChannelOption.TCP_NODELAY,true);
        callBackThread.start();
        try {
            ChannelFuture future = b.connect().sync();
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * The api for join the cluster naming @clusterName,
     * this api will bring 2 extra effect
     * 1.the change of the cluster will be notified,then onMemberFails will be called.This implies the registerWatcher is automatically called.
     * 2.once join successfully, the alive ip:port in the cluster will be passed to onJoinCluster as a parameter, the logs will be another parameter.
     *
     * param selfId the name of the cluster the caller want to join, it is designed for server which want to provide service with the help of raft
     */

    //这个版本专门给client调用,是要阻塞的.当返回的时候则onBecomeLeader已经执行完了.
    //当leader挂掉的时候,其实也会重选.但是如果在callBack线程里阻塞,那么就死锁了.所以目前leader挂掉的callBack只能不死锁.但不知到会不会带来什么问题?
    public void electLeader() {
        int requestIdx = requestOrder.getAndIncrement();
        epoch += 1;
        RaftRequest raftRequest = new SelectLeaderRequest(requestIdx,epoch,selfAddress,this);
        sendRequest(raftRequest,requestIdx);
        raftRequest.waitForResponse();
    }

    //这个版本专门给callBack线程调用,他不会阻塞,也不能阻塞
    public void electLeader(int epoch) {
        int requestIdx = requestOrder.getAndIncrement();
        RaftRequest raftRequest = new SelectLeaderRequest(requestIdx,epoch,selfAddress,this);
        sendRequest(raftRequest,requestIdx);
    }

    public void setSlot(int index, String content){
        int requestIdx = requestOrder.getAndIncrement();
        RaftRequest raftRequest = new SetSlotRequest(requestIdx,index,content);
        sendRequest(raftRequest,requestIdx);
        raftRequest.waitForResponse();
    }

    public void joinCLuster(String append) {
        int requestIdx = requestOrder.getAndIncrement();
        RaftRequest raftRequest = new JoinClusterRequest(requestIdx,append);
        sendRequest(raftRequest,requestIdx);
        raftRequest.waitForResponse();
    }

    public void leaveCLuster(String info) {
        int requestIdx = requestOrder.getAndIncrement();
        RaftRequest raftRequest = new LeaveClusterRequest(requestIdx,info);
        sendRequest(raftRequest,requestIdx);
        raftRequest.waitForResponse();
    }

    public ArrayList<String> getCurrentSlot() {
        int requestIdx = requestOrder.getAndIncrement();
        QuerySlotRequest raftRequest = new QuerySlotRequest(requestIdx);
        sendRequest(raftRequest,requestIdx);
        raftRequest.waitForResponse();
        return raftRequest.getResult();
    }

    public ArrayList<String> getCurrentAlive() {
        int requestIdx = requestOrder.getAndIncrement();
        QueryAliveRequest raftRequest = new QueryAliveRequest(requestIdx);
        sendRequest(raftRequest,requestIdx);
        raftRequest.waitForResponse();
        return raftRequest.getResult();
    }

    private void sendRequest(RaftRequest raftRequest,int requestIdx){
        while(ch==null){}
        ByteBuf byteBuf = getByteBuffer(raftRequest.toString());
        System.out.println("now send request  +  " + raftRequest.toString());
        RequestFactory.requestOnTheFly.put(requestIdx,raftRequest);
        ch.writeAndFlush(byteBuf);
    }

    ByteBuf getByteBuffer(String log){
        ByteBuf byteBuf = Unpooled.buffer(log.length() + 4);
        byteBuf.writeInt(log.length());
        byteBuf.writeBytes(log.getBytes());
        return byteBuf;
    }
}
