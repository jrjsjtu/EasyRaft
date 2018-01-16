package EasyRaft.state;

import EasyRaft.StateManager;
import org.jgroups.JChannel;

import java.util.*;

/**
 * Created by jrj on 17-10-30.
 */
public abstract class State implements RaftRpc{
    protected static final int clusterSize = 3;
    protected static StateManager stateManager;

    public static String selfID;

    protected static RaftLog lastLog = new RaftLog(0,0,"zero log");

    protected static long commitIndex = 0;
    protected static long lastApplied = -1;

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
            //stringBuilder.append(tmp.getTerm()).append(',').append(tmp.getIndex()).append(',').append(tmp.getLog()).append('|');
            stringBuilder.append(tmp.getLog()).append(';');
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

    public static void setStateManager(StateManager stateManager){
        State.stateManager = stateManager;
    }

    protected boolean isLastCandidate(String candidateId){
        return (votedFor == null) || votedFor.equals(candidateId);
    }

    /*
    public static void insertEntriesIntoLogs(LinkedList<String> entries){
        for (String entry:entries){
            insert0(entry);
        }
    }
    */
    public static void insertEntriesIntoLogs(LinkedList<RaftLog> entries){
        for (RaftLog entry:entries){
            entry.setTerm(currentTerm);
            entry.setIndex(lastLog.getIndex()+1);
            lastLog = entry;
            logs.add(entry);
        }
    }

    private static void insert0(String entry){
        long localIndex;
        localIndex = lastLog.getIndex();
        RaftLog newLog = new RaftLog(currentTerm,localIndex+1,entry);
        System.out.println("insert entry into log " + entry );
        lastLog = newLog;
        logs.add(newLog);
    }

    static void insertEntriesIntoLogs(byte[] entries){
        String string = new String(entries);
        String[] strArray = string.split(";");
        for (String entry:strArray) {
            insert0(entry);
        }
    }
}
