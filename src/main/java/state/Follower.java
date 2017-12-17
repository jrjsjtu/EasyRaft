package state;

import EasyRaft.RaftDelayedTask;
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
        stateManager.submitDelayed(new CheckHeartBeatTask(this,System.currentTimeMillis()+1000));
    }

    public String AppendEntries(long term, String leaderId, long prevLogIndex, long prevLogTerm, byte[] entries, long leaderCommit) {
        //System.out.println("AppendEntries " + term + " " + currentTerm);
        //如果 term < currentTerm 就返回 false
        //System.out.println("receive append rpc");
        if(leaderId.equals(leaderAddress)){
            receivedHeartbeat = true;
        }else{
            System.out.println("not equal " + leaderAddress + " " + leaderId);
        }
        if (term<currentTerm){
            return currentTerm+";False";
        }

        if (term > currentTerm) {
            currentTerm = term;
            leaderAddress = leaderId;
        }
        //point 2 in paper
        if (!checkIfInLogs(prevLogIndex,prevLogTerm)){
            //System.out.println("not in logs");
            return currentTerm+";False";
        }else{
            System.out.println("in logs");
            if (entries != null){
                insertEntriesIntoLogs(entries);
            }
            return currentTerm+";True";
        }
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

    public void insertEntriesIntoLogs(byte[] entries){
        if(logs.get(currentTerm)== null){
            logs.put(currentTerm,new ArrayList<String>());
        }
        System.out.println("new entry added " + new String(entries));
        logs.get(currentTerm).add(new String(entries));
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
