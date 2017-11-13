package state;

import Utils.Timeout;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import Utils.Timeout;
import Utils.TimerTask;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.Random;
/**
 * Created by jrj on 17-10-30.
 */
public class Follower extends State {
    protected String leaderAddress;
    protected Timeout timeoutForPeriodic;
    protected boolean receivedHeartbeat;

    public Follower(){
        System.out.println("become follower");
        receivedHeartbeat = false;
        timeoutForPeriodic = hashedWheelTimer.newTimeout(new PeriodicTask(),2000, TimeUnit.MILLISECONDS);
    }
    public Follower(String leaderAddress){
        System.out.println("become follower");
        this.leaderAddress = leaderAddress;
        receivedHeartbeat = false;
        timeoutForPeriodic = hashedWheelTimer.newTimeout(new PeriodicTask(),2000, TimeUnit.MILLISECONDS);
    }

    protected class PeriodicTask implements TimerTask{
        public void run(Timeout timeout) throws Exception {
            synchronized (mainWorker) {
                if (!receivedHeartbeat) {
                    System.out.println("no heartBeat and become candidate");
                    mainWorker.setState(new Candidate());
                } else {
                    receivedHeartbeat = false;
                    timeoutForPeriodic = hashedWheelTimer.newTimeout(new PeriodicTask(), 2000, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    @Override
    public String AppendEntries(long term, String leaderId, long prevLogIndex, long prevLogTerm, byte[] entries, long leaderCommit) {
        //point 1 in paper
        System.out.println("AppendEntries " + term + " " + currentTerm);
        if (term<currentTerm){
            return currentTerm+";False";
        }else{
            receivedHeartbeat = true;
        }
        if (term > currentTerm) {
            currentTerm = term;
            leaderAddress = leaderId;
        }
        //point 2 in paper
        if (!checkIfInLogs(prevLogIndex,prevLogTerm)){
            System.out.println("not in logs");
            return currentTerm+";False";
        }else{
            System.out.println("in logs");
            if (entries != null){
                insertEntriesIntoLogs(entries);
            }
            return currentTerm+";True";
        }
    }

    public void insertEntriesIntoLogs(byte[] entries){
        if(logs.get(currentTerm)== null){
            logs.put(currentTerm,new ArrayList<String>());
        }
        System.out.println("new entry added " + new String(entries));
        logs.get(currentTerm).add(new String(entries));
    }
    @Override
    public String RequestVote(long term, String candidateId, long lastLogIndex, long lastTerm) {
        if (term<currentTerm){
            return currentTerm + ";False";
        }
        if(isLastCandidate(candidateId) && isUpToDate(lastLogIndex,lastTerm)){
            currentTerm = term;
            votedFor = candidateId;
            leaderAddress = candidateId;
            return currentTerm + ";True";
        }
        return currentTerm + ";False";
    }
}
