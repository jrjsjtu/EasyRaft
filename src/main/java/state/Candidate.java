package state;

import EasyRaft.AppendRpcResult;
import EasyRaft.RaftDelayedTask;
import EasyRaft.StateManager;
import EasyRaft.VoteRpcResult;
import Utils.Timeout;
import Utils.TimerTask;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import worker.MainWorker;

import java.util.*;

/**
 * Created by jrj on 17-10-30.
 */

public class Candidate extends State {
    public Candidate(){
        System.out.println("become candidate");
        votedFor = selfID;
        currentTerm++;
        submitVoteTask();
    }

    public void submitVoteTask(){
        new VoteTask(this,System.currentTimeMillis()+500).run();
    }

    public String AppendEntries(long term, String leaderId, long prevLogIndex, long prevLogTerm, byte[] entries, long leaderCommit) {
        currentTerm = term;
        System.out.println("Candidate append entries "+term + "  " + currentTerm);
        Follower leaderFollower = new Follower(leaderId);
        stateManager.setState(leaderFollower);
        return leaderFollower.AppendEntries(term,leaderId,prevLogIndex,prevLogTerm,entries,leaderCommit);
    }

    public String RequestVote(long term, String candidateId, long lastLogIndex, long lastLogTerm) {
        //在candidate中,这里只能投自己,否则就要变成Follower
        System.out.println("Candidate "+term + "  " + currentTerm);
        if (term<currentTerm){
            return currentTerm + ";False";
        }else if(term>currentTerm){
            System.out.println("term > currentTerm");
            currentTerm = term;
            Follower leaderFollower = new Follower(candidateId);
            stateManager.setState(leaderFollower);
            return leaderFollower.RequestVote(term,candidateId,lastLogIndex,lastLogTerm);
        }else if(candidateId.equals(selfID)) {//这里隐含了term == currentTerm的意思
            return currentTerm + ";True;";
        }
        return currentTerm + ";False";
    }

    public void processVoteRpcResult(RspList rspList){
        if (rspList==null){
            currentTerm ++;
            stateManager.submitDelayed(new VoteTask(this,System.currentTimeMillis()+getRandomTimeout()));
            return;
        }
        System.out.println("VoteRpcResult result");
        Iterator iter = rspList.entrySet().iterator();
        int count = 0;
        while (iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            //UUID uuid = (UUID)entry.getKey();
            Rsp val = (Rsp)entry.getValue();
            String response = (String)val.getValue();
            /*
            if(response != null && response.equals("self rpc")){
                System.out.println(response);
                continue;
            }
            */
            if (response != null && response.split(";")[1].equals("True")){
                System.out.println(response);
                count +=1;
            }
        }
        //System.out.println("count is "+  count + " rsplist size is "+rspList.size());
        if (count>= clusterSize/2){
            Leader leader = new Leader();
            stateManager.setState(leader);
        }else{
            currentTerm ++;
            //虽然可以通过rpc的超时时间来实现随机超时,但是由于jgroups的缘故,所有的rpc几乎是马上返回的.因此只能在IO上返回.
            stateManager.submitDelayed(new VoteTask(this,System.currentTimeMillis()+getRandomTimeout()));
        }
    }


    private int getRandomTimeout(){
        Random random = new Random();
        //random [120,150]
        int s = random.nextInt(31) + 120;
        return s;
    }
    public class VoteTask extends RaftDelayedTask{
        public VoteTask(State state,long time){
            super(state,time);
        }
        public void run(){
            try{
                MethodCall call=new MethodCall(StateManager.class.getMethod("RequestVote",
                        long.class, String.class,long.class,long.class));
                RequestOptions opts=new RequestOptions(ResponseMode.GET_ALL, 4000);
                call.setArgs(currentTerm,selfID,lastLog.getIndex(),lastLog.getTerm());
                stateManager.submitIO(new VoteIOTask(opts,call));
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        private class VoteIOTask implements Runnable{
            RequestOptions opts;MethodCall call;
            VoteIOTask(RequestOptions opts,MethodCall call){
                this.opts = opts;this.call = call;
            }
            public void run() {
                RspList rsp_list= null;
                try {
                    List arrayList = stateManager.getMemberList();
                    if (arrayList.size() != 0){
                        rsp_list = stateManager.getRpcDispatcher().callRemoteMethods(arrayList, call, opts);
                    }else{
                        rsp_list = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stateManager.submitRpcResult(new VoteRpcResult(state,rsp_list));
            }
        }
    }
}
