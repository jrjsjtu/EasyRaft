package state;

import Utils.Timeout;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import Utils.Timeout;
import Utils.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.Random;
/**
 * Created by jrj on 17-10-30.
 */
public class Follower extends State {
    protected String leaderAddress;
    protected Timeout timeoutForPeriodic;
    protected boolean receivedHeartbeat;
    public Follower(String leaderAddress){
        this.leaderAddress = leaderAddress;
        receivedHeartbeat = false;
        timeoutForPeriodic = hashedWheelTimer.newTimeout(new PeriodicTask(),2000, TimeUnit.MILLISECONDS);
    }

    protected class PeriodicTask implements TimerTask{
        public void run(Timeout timeout) throws Exception {
            if (!receivedHeartbeat){
                synchronized (mainWorker){
                    mainWorker.setState(new Candidate());
                }
            }else{
                receivedHeartbeat = false;
                timeoutForPeriodic = hashedWheelTimer.newTimeout(new PeriodicTask(),2000, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public String AppendEntries(long term, String leaderId, int prevLogIndex, int prevLogTerm, byte[] entries, long leaderCommit) {
        if (term > currentTerm){
            currentTerm = term;
            leaderAddress = leaderId;
            receivedHeartbeat = true;
            if (lastLog.getTerm() == prevLogTerm && lastLog.getTerm() == prevLogTerm){
                return currentTerm + ";True";
            }
        }
        if (term == currentTerm && leaderId.equals(leaderAddress)){
            receivedHeartbeat = true;
            if (lastLog.getTerm() == prevLogTerm && lastLog.getTerm() == prevLogTerm){
                return currentTerm + ";True";
            }
        }
        return currentTerm + ";False";
    }

    @Override
    public String RequestVote(long term, String candidateId, int lastLogIndex, int lastTerm) {
        if (term<currentTerm){
            return currentTerm + ";False";
        }
        if(isLastCandidate(candidateId) && isUpToDate(lastLogIndex,lastTerm)){
            currentTerm = term;
            votedFor = candidateId;
            return currentTerm + ";True";
        }
        return currentTerm + ";False";
    }
}
