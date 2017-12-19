package state;

import EasyRaft.RaftDelayedTask;
import Utils.RaftArray;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import java.util.ArrayList;

/**
 * Created by jrj on 17-10-30.
 */
public class Follower extends State {
    protected String leaderAddress;
    private boolean receivedHeartbeat;

    public Follower(){
        System.out.println(System.currentTimeMillis() + "become follower");
        receivedHeartbeat = false;
        stateManager.submitDelayed(new CheckHeartBeatTask(this,System.currentTimeMillis()+1000));
    }

    public Follower(String leaderAddress){
        System.out.println("become follower " + leaderAddress);
        this.leaderAddress = leaderAddress;
        receivedHeartbeat = false;
        votedFor = null;
        unISRLog = new RaftArray();
        stateManager.submitDelayed(new CheckHeartBeatTask(this,System.currentTimeMillis()+1000));
    }
    public RaftArray unISRLog;
    public String AppendEntries(long term, String leaderId, long prevLogIndex, long prevLogTerm, byte[] entries, long leaderCommit) {
        //如果接收到的 RPC 请求或响应中，任期号T > currentTerm，那么就令 currentTerm 等于 T，并切换状态为跟随者
        if (term > currentTerm) {
            currentTerm = term;
            leaderAddress = leaderId;
        }
        //重置receivedHeartbeat只适用于leader发来的心跳
        if(leaderId.equals(leaderAddress)){
            receivedHeartbeat = true;
        }

        //if (entries != null){System.out.println("receive " + new String(entries));}
        //1.如果 term < currentTerm 就返回 false （5.1 节）
        if (term<currentTerm){
            return currentTerm+";False";
        }

        //2.如果日志在 prevLogIndex 位置处的日志条目的任期号和 prevLogTerm 不匹配，则返回 false （5.3 节）
        if (!checkIfInLogs(prevLogIndex,prevLogTerm)) {
            //insertEntriesIntoLogs(entries);
            //System.out.println("not in logs");
            return currentTerm + ";False";
        }

        //3.如果日志在 prevLogIndex 位置处的日志条目的任期号和 prevLogTerm 不匹配，则返回 false （5.3 节）
        removeExtraLog(prevLogIndex);
        //4.附加任何在已有的日志中不存在的条目
        if (entries != null){
            insertEntriesIntoLogs(entries);
        }
        return currentTerm+";True";
    }

    public String RequestVote(long term, String candidateId, long lastLogIndex, long lastTerm) {
        //System.out.println("Follower" + term + "  " + currentTerm);
        if (term<currentTerm){
            return currentTerm + ";False";
        }
        //if(isLastCandidate(candidateId) && isUpToDate(lastLogIndex,lastTerm)){
        if(isLastCandidate(candidateId)){
            System.out.println("Follower voted for " + candidateId);
            currentTerm = term;
            votedFor = candidateId;
            leaderAddress = candidateId;
            return currentTerm + ";True";
        }
        return currentTerm + ";False";
    }

    
    public class CheckHeartBeatTask extends RaftDelayedTask{
        CheckHeartBeatTask(State state,long time){
            super(state,time);
        }

        public void run() {
            //在worker线程中
            //System.out.println(state);System.out.println(stateManager.getState());
            System.out.println(System.currentTimeMillis() + "  check heart beat "  + receivedHeartbeat);
            if (receivedHeartbeat){
                receivedHeartbeat = false;
                stateManager.submitDelayed(new CheckHeartBeatTask(state,System.currentTimeMillis()+1000));
            }else{
                Candidate candidate  = new Candidate();
                stateManager.setState(candidate);
            }
        }
    }
}
