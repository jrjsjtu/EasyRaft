package EasyRaft.client;

import EasyRaft.client.callBack.RaftCallBack;
import EasyRaft.requests.JoinClusterRequest;
import EasyRaft.requests.QuerySlotRequest;
import EasyRaft.requests.RaftRequest;
import EasyRaft.requests.SelectLeaderRequest;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by jrj on 18-1-31.
 */
public class CtxProxy{
    public static int[] portList;

    EventLoopGroup group;
    String appendInfo;


    public CtxProxy(String hostIp,String info){
        group = new NioEventLoopGroup(1);
        StringBuilder stringBuilder = new StringBuilder().append(hostIp).append(':');
        stringBuilder.append(info);

        appendInfo = stringBuilder.toString();

    }

    public CtxProxy(){
        group = new NioEventLoopGroup(1);
    }

    public void addCtx(RaftClient ctx){
        synchronized (successClient){
            successClient.add(ctx);
        }
    }

    public void removeCtx(RaftClient ctx){
        synchronized (successClient){
            successClient.remove(ctx);
        }
    }

    HashSet<RaftClient> raftClients = new HashSet<RaftClient>();
    HashSet<RaftClient> successClient = new HashSet<RaftClient>();

    Class<? extends RaftCallBack> callBackClass;
    public void setCallBackClass(Class<? extends RaftCallBack> callBackClass) {
        this.callBackClass = callBackClass;
    }

    public void tryToConnect(String ipAddress,int port) throws Exception{
        final RaftCallBack raftCallBack = callBackClass.newInstance();
        final RaftClient raftClient = new RaftClient(raftCallBack);
        raftClients.add(raftClient);
        raftClient.joinRaft(group,ipAddress,port,appendInfo,this,true);
        //raftClient.electLeader(stringBuilder.toString());
        //raftClient.joinCLuster(stringBuilder.toString());
    }

    public void connectWithoutHeartbeat(String ipAddress,int port) throws Exception{
        final RaftClient raftClient = new RaftClient();
        raftClients.add(raftClient);
        raftClient.joinRaft(group,ipAddress,port,appendInfo,this,false);
        //raftClient.electLeader(stringBuilder.toString());
        //raftClient.joinCLuster(stringBuilder.toString());
    }


    public ArrayList<String> querySlot(){
        int requestIdx = RaftClient.requestOrder.getAndIncrement();
        QuerySlotRequest querySlotRequest = new QuerySlotRequest(requestIdx);
        synchronized (successClient){
            for(RaftClient raftClient:successClient){
                raftClient.sendRequest(querySlotRequest,requestIdx);
            }
        }
        querySlotRequest.waitForResponse();
        return querySlotRequest.getResult();
    }

    public void joinCluster(){
        int requestIdx = RaftClient.requestOrder.getAndIncrement();
        JoinClusterRequest raftRequest = new JoinClusterRequest(requestIdx,appendInfo);
        synchronized (successClient){
            for(RaftClient raftClient:successClient){
                raftClient.sendRequest(raftRequest,requestIdx);
            }
        }
        raftRequest.waitForResponse();
    }

    public class ReconnectThread implements Runnable{
        public void run() {
            while(true){
                synchronized (successClient){
                    Iterator<RaftClient> iterator = raftClients.iterator();
                    while(iterator.hasNext()){
                        RaftClient raftClient = iterator.next();
                        if (!successClient.contains(raftClient)){
                            try {
                                raftClient.joinRaft();
                            }catch (Exception e){

                            }
                            System.out.println("join success");
                        }
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
