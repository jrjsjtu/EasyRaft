package state;

import EasyRaft.RaftDelayedTask;

/**
 * Created by jrj on 17-10-30.
 */
public class Follower extends State {
    protected String leaderAddress;
    private boolean receivedHeartbeat;

    public Follower(){
        System.out.println(System.currentTimeMillis() + "become follower");
        votedFor = null;
        receivedHeartbeat = false;
        stateManager.submitDelayed(new CheckHeartBeatTask(this,System.currentTimeMillis()+1000));
    }

    public Follower(String leaderAddress){
        System.out.println(System.currentTimeMillis() + "become follower " + leaderAddress);
        this.leaderAddress = leaderAddress;
        votedFor = null;
        receivedHeartbeat = false;
        stateManager.submitDelayed(new CheckHeartBeatTask(this,System.currentTimeMillis()+1000));
    }

    public String AppendEntries(long term, String leaderId, long prevLogIndex, long prevLogTerm, byte[] entries, long leaderCommit) {
        //如果接收到的 RPC 请求或响应中，任期号T > currentTerm，那么就令 currentTerm 等于 T，并切换状态为跟随者
        if (term > currentTerm) {
            currentTerm = term;
            leaderAddress = leaderId;
            votedFor = null;
        }
        //重置receivedHeartbeat只适用于leader发来的心跳
        if(leaderId.equals(leaderAddress)){
            receivedHeartbeat = true;
        }

        //if (entries != null){System.out.println("receive " + new String(entries));}
        //1.如果 term < currentTerm 就返回 false （5.1 节）
        if (term<currentTerm){
            return currentTerm+";False;"+lastLog.getIndex();
        }

        //2.如果日志在 prevLogIndex 位置处的日志条目的任期号和 prevLogTerm 不匹配，则返回 false （5.3 节）
        if (!checkIfInLogs(prevLogIndex,prevLogTerm)) {
            //insertEntriesIntoLogs(entries);
            //System.out.println("not in logs");
            return currentTerm + ";False;"+lastLog.getIndex();
        }

        //3.如果已经存在的日志条目和新的产生冲突（索引值相同但是任期号不同），删除这一条和之后所有的 （5.3 节）
        for (int i=logs.size()-1;i>prevLogIndex;i--){
            logs.remove(i);
        }
        //4.附加任何在已有的日志中不存在的条目
        //3,4由insertEntriesIntoLogs完成
        if (entries != null){
            insertEntriesIntoLogs(entries);
        }
        commitIndex = leaderCommit;
        for(long i=lastApplied+1;i<=commitIndex;i++){
            System.out.println("execute " + i);
        }
        lastApplied = commitIndex;
        return currentTerm+";True;"+lastLog.getIndex();
    }

    public String RequestVote(long term, String candidateId, long lastLogIndex, long lastTerm) {
        //System.out.println("Follower" + term + "  " + currentTerm);
        //如果term < currentTerm返回 false
        if (term<currentTerm){
            return currentTerm + ";False";
        }
        //如果接收到的 RPC 请求或响应中，任期号T > currentTerm，那么就令 currentTerm 等于 T，并切换状态为跟随者
        //如果 votedFor 为空或者就是 candidateId，并且候选人的日志至少和自己一样新，那么就投票给他
        if((term>currentTerm || isLastCandidate(candidateId)) && !lastLog.moreUpToDate(lastLogIndex,lastTerm)){
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
            //System.out.println(System.currentTimeMillis() + "  check heart beat "  + receivedHeartbeat);
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
