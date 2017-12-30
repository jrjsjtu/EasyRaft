package EasyRaft;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.View;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
import state.Candidate;
import state.Follower;
import state.Leader;
import state.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jrj on 17-12-11.
 */
public class StateManager {
    //delayQueue用来摆放延迟任务,由delayQueuePoller
    private DelayQueue delayQueue;
    private LinkedBlockingQueue taskQueue;
    private Thread workerThread;
    private Thread delayQueuePoller;
    private Executor IOExecutor;

    RpcDispatcher disp;
    JChannel channel;

    private State current;
    private View view;
    public StateManager(){
        delayQueue = new DelayQueue();
        taskQueue = new LinkedBlockingQueue();
        IOExecutor = Executors.newFixedThreadPool(4);
        workerThread = new Thread(new worker());
        delayQueuePoller = new Thread(new delayQueueWorker());
        workerThread.start();
        delayQueuePoller.start();
        joinJGroup();
        initState();
    }

    private void joinJGroup(){
        try{
            channel=new JChannel();
            disp=new RpcDispatcher(channel, this);
            disp.setMembershipListener(new ClusterInfoReceiver(this));
            channel.connect("RpcDispatcherTestGroup");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void initState(){
        State.setJChannel(channel);
        State.setStateManager(this);

        Follower follower = new Follower();
        current = follower;
    }

    public void setState(State tmpStat){
        this.current = tmpStat;
    }

    public void commit(final String entry, final Object ctx){
        submitRunnable(new Runnable() {
            public void run() {
                if (isLeader()){
                    ((Leader)current).insertEntryIntoList(entry,ctx);
                }else{
                    ByteBuf byteBuf = Unpooled.buffer();
                    byteBuf.writeInt(12);
                    byteBuf.writeBytes("wrong leader".getBytes());
                    ((ChannelHandlerContext)ctx).writeAndFlush(byteBuf);
                }
            }
        });
    }
    //这里与论文中的实现有所不同,在返回值中加入了,调用rpc时的index值,方便程序的阅读性
    //但我没有在每个状态的appendEntries里加,而是在这边的返回值中统一加
    public String AppendEntries(long term,String leaderId,long prevLogIndex,long prevLogTerm,byte[] entries,long leaderCommit,String target){
        if (leaderId.equals(State.selfID)) {
            return "self rpc";
        }
        if (!target.equals("all") && !target.equals(State.selfID)){
            System.out.println("rpc not target");
            return "self rpc";
        }
        AppendPara appendPara = new AppendPara(term,leaderId,prevLogIndex,prevLogTerm,entries,leaderCommit);
        try {
            synchronized (appendPara){
                //这里应该把入队操作放在synchronized内,花了我一天的时间来debug.不然就有可能出现notify在wait之前被调用!
                taskQueue.put(appendPara);
                appendPara.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return appendPara.getResult()+";"+prevLogIndex;
    }

    public String RequestVote(long term,String candidateId,long lastLogIndex,long lastLogTerm){
        if (candidateId.equals(State.selfID)) {
            return "self rpc";
        }
        VotePara votePara = new VotePara(term,candidateId,lastLogIndex,lastLogTerm);
        try {
            synchronized (votePara){
                //这里应该把入队操作放在synchronized内,花了我一天的时间来debug.
                taskQueue.put(votePara);
                votePara.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return votePara.getResult();
    }


    public RpcDispatcher getRpcDispatcher(){
        return disp;
    }

    public boolean isCandidate(){
        return (current instanceof Candidate);
    }

    public boolean isLeader(){
        return (current instanceof Leader);
    }

    public boolean isFollower(){
        return (current instanceof Follower);
    }


    public void submitIO(Runnable runnable){
        IOExecutor.execute(runnable);
    }

    public void submitRunnable(Runnable runnable){
        try {
            taskQueue.put(runnable);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void submitRpcResult(Object result){
        try {
            taskQueue.put(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void submitDelayed(Delayed delayed){
        delayQueue.put(delayed);
    }

    public State getState(){
        return current;
    }

    private class delayQueueWorker implements Runnable{
        public void run() {
           while(true){
               try {
                   Delayed delayed = delayQueue.take();
                   submitRunnable((Runnable) delayed);
                   /*
                   if (delayed instanceof Candidate.VoteTask){
                       submitIO((Runnable)delayed);
                   }else if(delayed instanceof Follower.CheckHeartBeatTask){
                       submitRunnable((Runnable) delayed);
                   }else if(delayed instanceof Leader.HeartBeatSendTask){
                       submitIO((Runnable) delayed);
                   }
                   */
               } catch (Exception e) {
                   e.printStackTrace();
               }
           }
        }
    }

    private class worker implements Runnable{
        public void run() {
            while(true){
                try {
                    Object rpcPara = taskQueue.take();
                    synchronized (rpcPara){
                        if (rpcPara instanceof AppendPara){
                            AppendPara appendPara = (AppendPara)rpcPara;
                            while(current==null){
                                //有可能current还没有初始化,就收到rpc了,若出现这种情况就先自旋一会儿
                            }
                            String result = current.AppendEntries(appendPara.getTerm(),appendPara.getLeaderId(),appendPara.getPrevLogIndex(),
                                    appendPara.getPrevLogTerm(),appendPara.getEntries(),appendPara.getLeaderCommit());
                            appendPara.setResult(result);
                            appendPara.notifyAll();
                        }else if(rpcPara instanceof VotePara){
                            VotePara appendPara = (VotePara)rpcPara;
                            while(current==null){
                                //有可能current还没有初始化,就收到rpc了,若出现这种情况就先自旋一会儿
                            }
                            String result = current.RequestVote(appendPara.getTerm(),appendPara.getCandidateId(),appendPara.getLastLogIndex(), appendPara.getLastLogTerm());
                            appendPara.setResult(result);
                            appendPara.notifyAll();
                        }else if(rpcPara instanceof AppendRpcResult){
                            AppendRpcResult appendRpcResult = (AppendRpcResult)(rpcPara);
                            if (current == appendRpcResult.getReceiver() && current instanceof Leader){
                                ((Leader)current).processAppendRpcResult(appendRpcResult.getRspList());
                            }
                        }else if(rpcPara instanceof VoteRpcResult){
                            VoteRpcResult voteRpcResult = (VoteRpcResult)(rpcPara);
                            if (current == voteRpcResult.getReceiver() && current instanceof Candidate){
                                ((Candidate)current).processVoteRpcResult(voteRpcResult.getRspList());
                            }
                        }else if(rpcPara instanceof RaftDelayedTask){
                            RaftDelayedTask raftDelayedTask = (RaftDelayedTask)rpcPara;
                            if (raftDelayedTask.stillLastState(current)){
                                ((Runnable)(rpcPara)).run();
                            }
                        }else if(rpcPara instanceof Runnable){
                            ((Runnable)(rpcPara)).run();
                        } else{
                            System.out.println("illegal type");
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
