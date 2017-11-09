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
    RspList rsp_list;

    JChannel channel;

    public MainWorker() throws Exception{
        //State.setJChannel(channel);
        //current = new Candidate();
        //State.setMainWorker(this);
        //((Candidate)current).joinGroup();
    }

    public static int print(int number) throws Exception {
        return number * 2;
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
        MethodCall call=new MethodCall(getClass().getMethod("AppendEntries",
                long.class, String.class,int.class,int.class,byte[].class,long.class));
        RequestOptions opts=new RequestOptions(ResponseMode.GET_ALL, 5000);
        channel=new JChannel();
        disp=new RpcDispatcher(channel, this);
        channel.connect("RpcDispatcherTestGroup");

        for(int i=0; i < 1; i++) {
            Util.sleep(100);
            call.setArgs(1l,"fuck",1,2,"fuck".getBytes(),1l);
            rsp_list=disp.callRemoteMethods(null, call, opts);
            System.out.println("Responses: " + rsp_list);
        }
        Util.close(disp, channel);
    }
    public void setState(State state){
        current = state;
    }

    public String AppendEntries(long term,String leaderId,int prevLogIndex,int prevLogTerm,byte[] entries,long leaderCommit){
        return "fuck";
    }

    public String RequestVote(long term,String candidateId,int lastLogIndex,int lastLogTerm){
        return null;
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
