package state;

import Utils.HashedWheelTimer;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import java.util.*;
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

    protected static long currentTerm = -1l;
    protected static String votedFor;

    protected static HashMap<Long,ArrayList<String>> logs = new HashMap<Long, ArrayList<String>>();
    protected static RaftLog lastLog = new RaftLog(-1,-1,null);
    protected static long commitIndex;

    protected static HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();

    public static void setJChannel(JChannel jChannel){
        State.selfID = jChannel.getAddress().toString();
        State.jChannel = jChannel;
    }

    public static boolean checkIfInLogs(long lastLogIndex,long lastLogTerm){
        // here only term's size is int is supported.
        if (lastLogIndex == -1 && lastLogTerm == -1){return true;}
        if (logs.get(lastLogTerm)==null){
            return false;
        }else{
            ArrayList<String> logInOneTerm = logs.get(lastLogTerm);
            if (logInOneTerm.size()<=lastLogIndex) return false;
            else{
                //point 3 in paper
                Iterator iter = logs.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    Long key = (Long)entry.getKey();
                    if (key>lastLogTerm){
                        iter.remove();
                    }
                }
                while (lastLogTerm+1 < logs.size()){
                    logs.remove(logs.get(logs.size()-1));
                }
                while (lastLogIndex+1 < logInOneTerm.size()){
                    logInOneTerm.remove(logInOneTerm.get(logInOneTerm.size()-1));
                }
                return true;
            }
        }
    }
    public static void setMainWorker(MainWorker mainWorker){
        State.mainWorker = mainWorker;
    }

    protected boolean isLastCandidate(String candidateId){
        return (votedFor == null) || votedFor.equals(candidateId);
    }

    protected boolean isUpToDate(long lastLogIndex,long lastLogTerm){
        if (lastLog.getTerm()<lastLogTerm){
            return true;
        }else if(lastLog.getTerm() == lastLogIndex && lastLog.getIndex()<=lastLogIndex){
            return true;
        }
        return false;
    }

    public String AppendEntries(long term, String leaderId, long prevLogIndex, long prevLogTerm, byte[] entries, long leaderCommit) {
        return null;
    }

    public String RequestVote(long term, String candidateId, long lastLogIndex, long lastLogTer) {
        return null;
    }

}
