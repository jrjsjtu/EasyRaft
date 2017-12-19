package state;

import EasyRaft.StateManager;
import Utils.RaftArray;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import java.util.*;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by jrj on 17-10-30.
 */
public abstract class State implements RaftRpc{
    protected static final int clusterSize = 3;
    protected static StateManager stateManager;

    public static String selfID;

    protected static RaftLog lastLog = new RaftLog(0,0,"zero log");

    protected static long commitIndex;
    protected static long lastApplied;

    protected static long currentTerm = 0;
    protected static String votedFor;
    protected static ArrayList<RaftLog> logs = new ArrayList<RaftLog>();
    static{
        logs.add(lastLog);
    }
    //protected static HashMap<Long,ArrayList<String>> logs = new HashMap<Long, ArrayList<String>>();

    public static RaftLog getLog(long index){
        return logs.get((int)index);
    }
    public static void setJChannel(JChannel jChannel){
        State.selfID = jChannel.getAddress().toString();
    }

    public static byte[] getByteForFollower(long index){
        StringBuilder stringBuilder = new StringBuilder();
        for (int i=(int)index+1;i<logs.size();i++){
            RaftLog tmp = logs.get(i);
            stringBuilder.append(tmp.getTerm()).append(',').append(tmp.getIndex()).append(',').append(tmp.getLog()).append('|');
        }
        stringBuilder.deleteCharAt(stringBuilder.length()-1);
        return stringBuilder.toString().getBytes();
    }

    public static boolean checkIfInLogs(long lastLogIndex,long lastLogTerm){
        // here only term's size is int is supported.
        if (logs.size()<=lastLogIndex){
            return false;
        }
        //System.out.println(logs.size() + "   " + lastLogIndex);
        RaftLog raftLog = logs.get((int)lastLogIndex);
        long term =  raftLog.getTerm();
        /*
        if (term!=lastLogTerm){
            for (int i=logs.size()-1;i>=lastLogIndex;i--){
                logs.remove(i);
            }
            lastLog = logs.get(logs.size()-1);
        }
        */
        return term==lastLogTerm;
    }

    public static void removeExtraLog(long lastLogIndex){
        int endPosition = logs.size()-1;
        for (int i=endPosition;i>lastLogIndex;i--){
            logs.remove(i);
        }
    }

    public static void setStateManager(StateManager stateManager){
        State.stateManager = stateManager;
    }

    protected boolean isLastCandidate(String candidateId){
        return (votedFor == null) || votedFor.equals(candidateId);
    }

    public static void insertEntriesIntoLogs(byte[] entries){
        String[] tmpEntryList = new String(entries).split("\\|");
        for (String entry:tmpEntryList){
            String[] raftInfo = entry.split(",");
            long raftTerm,raftIndex;
            raftTerm = Long.parseLong(raftInfo[0]);
            raftIndex = Long.parseLong(raftInfo[1]);
            System.out.println("insert entry into log " + entry );
            RaftLog newLog = new RaftLog(raftTerm,raftIndex,raftInfo[2]);
            lastLog = newLog;
            logs.add(newLog);
        }
    }
}
