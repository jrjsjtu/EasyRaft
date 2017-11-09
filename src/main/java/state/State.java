package state;

import Utils.HashedWheelTimer;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by jrj on 17-10-30.
 */
public class State implements RaftRpc{
    protected static JChannel jChannel;
    protected static MainWorker mainWorker;
    protected static String selfID;
    protected static final int clusterSize = 3;

    protected static long currentTerm = 0l;
    protected static String votedFor;

    protected static LinkedBlockingQueue logs = new LinkedBlockingQueue();
    protected static RaftLog lastLog = new RaftLog(-1,-1,null);
    protected static long commitIndex;

    protected static HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();
    public static void setJChannel(JChannel jChannel){
        State.selfID = jChannel.getAddress().toString();
        State.jChannel = jChannel;
    }

    public static void setMainWorker(MainWorker mainWorker){
        State.mainWorker = mainWorker;
    }

    public String AppendEntries(long term, String leaderId, int prevLogIndex, int prevLogTerm, byte[] entries, long leaderCommit) {
        return null;
    }

    protected boolean isLastCandidate(String candidateId){
        return (votedFor == null) || votedFor.equals(candidateId);
    }

    protected boolean isUpToDate(int lastLogIndex,int lastLogTerm){
        if (lastLog.getTerm()<lastLogTerm){
            return true;
        }else if(lastLog.getTerm() == lastLogIndex && lastLog.getIndex()<=lastLogIndex){
            return true;
        }
        return false;
    }

    protected boolean isNewer(int lastLogIndex,int lastLogTerm){
        if (lastLog.getTerm()<lastLogTerm){
            return true;
        }else if(lastLog.getTerm() == lastLogIndex && lastLog.getIndex()<lastLogIndex){
            return true;
        }
        return false;
    }


    public String RequestVote(long term, String candidateId, int lastLogIndex, int lastLogTer) {
        return null;
    }
}
