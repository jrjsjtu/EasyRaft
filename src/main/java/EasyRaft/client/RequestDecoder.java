package EasyRaft.client;

import EasyRaft.client.callBack.RaftCallBack;
import EasyRaft.requests.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

/**
 * Created by jrj on 17-12-29.
 */
public class RequestDecoder extends ChannelInboundHandlerAdapter {
    int payLoadSize;
    ByteBuf header,payLoad;

    RaftClient raftClient;
    RaftCallBack raftCallBack;

    public RequestDecoder(RaftClient raftClient, RaftCallBack raftCallBack){
        this.raftClient = raftClient;
        this.raftCallBack = raftCallBack;

        header = Unpooled.buffer(4);
    }


    private final static char headerReading = '1';
    private final static char payLoadReading = '2';
    private char currentState = '1';
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        if(buf.readableBytes()>0 && currentState == headerReading) {
            buf.readBytes(header);
            if (header.writableBytes() == 0) {
                payLoadSize = header.readInt();
                payLoad = Unpooled.buffer(payLoadSize);
                currentState = payLoadReading;
            }
        }

        if(buf.readableBytes()>0 && currentState == payLoadReading) {
            buf.readBytes(payLoad);
            if (payLoad.writableBytes() == 0){
                //processPayLoad里会抛出各种异常的!
                try{
                    processPayLoad(payLoad,ctx);
                }catch (Exception e){
                    e.printStackTrace();
                }

                payLoad.release();
                currentState = headerReading;
                header.resetWriterIndex();
                header.resetReaderIndex();
            }
        }

        if(buf.readableBytes()==0){
            buf.release();
        } else{
            this.channelRead(ctx,msg);
        }
    }

    private void processPayLoad(ByteBuf payLoad,ChannelHandlerContext ctx) throws Exception{
        char c = (char) payLoad.readByte();
        byte[] bytes = new byte[payLoad.readableBytes()];
        payLoad.readBytes(bytes);
        System.out.println("receive response + " +c + " "+ new String(bytes));
        //RegisterMember以及RegisterWatcher的返回结果总是相同的
        switch (c){
            case RaftClient.SelectLeaderResponse:
                //request index|epoch|address
                processLeaderSelect(bytes);
                break;
            case RaftClient.SetSlotResponse:
                //request index|id|content
                processSetSlot(bytes);
                break;
            case RaftClient.NotifyLeaderDown:
                //epoch
                processLeaderDown(bytes);
                break;
            case RaftClient.QueryAliveResponse:
                //request id|ip:port:sharding|ip:port:sharding
                processQueryResponse(bytes);
                break;
            case RaftClient.QuerySlotResponse:
                //request id|ip:port:sharding|null|ip:port:sharding
                processQueryResponse(bytes);
                break;
            case RaftClient.NotifyMemberDown:
                //ip:port:sharding
                processMemberDown(bytes);
                break;
            case RaftClient.NotifyMemberUp:
                //ip:port:sharding
                processMemberUp(bytes);
                break;
            case RaftClient.JoinClusterResponse:
                processJoinCluster(bytes);
                break;
            case RaftClient.LeaveClusterResponse:
                processLeaveCluster(bytes);
                break;
        }
    }
    private void processJoinCluster(byte[] bytes){
        singleProcess(bytes);
    }

    private void processLeaveCluster(byte[] bytes){
        singleProcess(bytes);
    }

    private void singleProcess(byte[] bytes){
        String[] info = new String(bytes).split("\\|");
        int requestId = Integer.parseInt(info[0]);
        AbstractRequest raftRequest = (AbstractRequest) RequestFactory.requestOnTheFly.get(requestId);
        raftRequest.notifyResponse();
        RequestFactory.requestOnTheFly.remove(requestId);
    }

    //这两边过来都是一样的格式,只是alive回来是没有null的.而slot是有null的.
    private void processQueryResponse(byte[] bytes){
        String info = new String(bytes);
        String[] infos = info.split("\\|");
        int requestIdx = Integer.parseInt(infos[0]);
        ArrayList<String> arrayList = new ArrayList<String>();
        for(int i=1;i<infos.length;i++){
            arrayList.add(infos[i]);
        }
        RaftRequest raftRequest = RequestFactory.requestOnTheFly.get(requestIdx);
        if (raftRequest instanceof QueryAliveRequest){
            QueryAliveRequest queryAliveRequest = (QueryAliveRequest)raftRequest;
            queryAliveRequest.setResult(arrayList);
            queryAliveRequest.notifyResponse();
        }else if(raftRequest instanceof QuerySlotRequest){
            QuerySlotRequest querySlotRequest = (QuerySlotRequest)raftRequest;
            querySlotRequest.setResult(arrayList);
            querySlotRequest.notifyResponse();
        }
    }

    //这里两个请求是没有RequestId的
    private void processMemberDown(byte[] bytes){
        String[] infos = new String(bytes).split(":");
        MemberDownRequest memberDownRequest = new MemberDownRequest(new String(bytes));
        try {
            raftClient.callBackTask.put(memberDownRequest);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void processMemberUp(byte[] bytes){
        String[] infos = new String(bytes).split(":");
        int shard= Integer.parseInt(infos[2]);
        MemberUpRequest memberUpRequest = new MemberUpRequest(shard,new String(bytes));
        try {
            raftClient.callBackTask.put(memberUpRequest);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void processLeaderDown(byte[] bytes){
        int epoch = Integer.parseInt(new String(bytes));
        LeaderDownRequest leaderDownRequest = new LeaderDownRequest(epoch);
        try {
            raftClient.callBackTask.put(leaderDownRequest);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void processLeaderSelect(byte[] bytes) throws Exception{
        //request index|epoch|address
        String result = new String(bytes);
        String[] tmp = result.split("\\|");
        int requestIdx = Integer.parseInt(tmp[0]);
        int epoch = Integer.parseInt(tmp[1]);
        SelectLeaderRequest raftRequest =(SelectLeaderRequest)RequestFactory.requestOnTheFly.get(requestIdx);
        if (tmp[2].equals("null") || epoch<raftClient.getEpoch()){
            //发现leader挂了,就再选一次.
            int targetEpoch = Math.max(raftClient.getEpoch()+1,epoch+1);
            raftRequest.setEpoch(targetEpoch);
            raftClient.setEpoch(targetEpoch);
            ByteBuf byteBuf = raftClient.getByteBuffer(raftRequest.toString());
            ctx.writeAndFlush(byteBuf);
            System.out.println("null leader. Start a new select");
        }else if(raftClient.getEpoch() == epoch && tmp[2].equals(raftClient.getSelfAddress())){
            //走到这里表示已经是leader了,把它给callBack线程处理.
            //然后删掉该request
            raftClient.callBackTask.put(raftRequest);
            RequestFactory.requestOnTheFly.remove(requestIdx);
        }else if(raftClient.getEpoch() == epoch && !tmp[2].equals(raftClient.getSelfAddress())){
            RequestFactory.requestOnTheFly.remove(requestIdx);
            raftRequest.notifyResponse();
            System.out.println("Not leader.End select");
        }else if(raftClient.getEpoch()>epoch){
            RequestFactory.requestOnTheFly.remove(requestIdx);
            raftRequest.notifyResponse();
            System.out.println("Not leader.End select");
        }else{
            RequestFactory.requestOnTheFly.remove(requestIdx);
            raftRequest.notifyResponse();
            System.out.println("Not leader.End select");
        }
    }

    private void processSetSlot(byte[] bytes){
        String result = new String(bytes);
        int requestIdx = Integer.parseInt(result.split("\\|")[0]);
        SetSlotRequest abstractRequest = (SetSlotRequest)RequestFactory.requestOnTheFly.get(requestIdx);
        RequestFactory.requestOnTheFly.remove(requestIdx);
        abstractRequest.notifyResponse();
    }


    ChannelHandlerContext ctx;
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String selfAddress = ctx.channel().localAddress().toString().substring(1);
        raftClient.setSelfAddress(selfAddress);
        this.ctx = ctx;
    }
}
