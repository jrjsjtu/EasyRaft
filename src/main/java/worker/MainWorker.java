package worker;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;
import state.Candidate;
import state.Follower;
import state.Leader;
import state.State;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by jrj on 17-10-30.
 */
public class MainWorker{
    private static final int clusterSize = 3;
    private State current;

    RpcDispatcher disp;

    JChannel channel;

    public MainWorker() throws Exception{
        //State.setJChannel(channel);
        //current = new Candidate();
        //State.setMainWorker(this);
    }

    public boolean isCandidate(){
        if (current instanceof Candidate){
            return true;
        }else
            return false;
    }

    public boolean isLeader(){
        if (current instanceof Leader){
            return true;
        }else
            return false;
    }
    public RpcDispatcher GetRpcDispacher(){
        return disp;
    }

    public void start() throws Exception {
        channel=new JChannel();
        disp=new RpcDispatcher(channel, this);
        channel.connect("RpcDispatcherTestGroup");
        State.setJChannel(channel);
        State.setMainWorker(this);

        current = new Follower();
    }

    public void setState(State state){
        current = state;
    }

    public String AppendEntries(long term,String leaderId,long prevLogIndex,long prevLogTerm,byte[] entries,long leaderCommit){
        synchronized (this){
            return current.AppendEntries(term,leaderId,prevLogIndex,prevLogTerm,entries,leaderCommit);
        }
    }

    public String RequestVote(long term,String candidateId,long lastLogIndex,long lastLogTerm){
        synchronized (this){
            return current.RequestVote(term,candidateId,lastLogIndex,lastLogTerm);
        }
    }

    public static void main(String[] args){
        try {
            MainWorker mainWorker = new MainWorker();
            mainWorker.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
