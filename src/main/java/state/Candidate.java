package state;

import Utils.Timeout;
import Utils.TimerTask;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import worker.MainWorker;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-10-30.
 */

public class Candidate extends State {
    Timeout timeoutForCandidate;

    public Candidate(){
        votedFor = selfID;
        hashedWheelTimer.newTimeout(new VoteTask(),0, TimeUnit.MILLISECONDS);
        System.out.println("become candidate");
    }

    public String AppendEntries(long term, String leaderId, int prevLogIndex, int prevLogTerm, byte[] entries, long leaderCommit) {
        if (term>currentTerm){
            currentTerm = term;
            Follower leaderFollower = new Follower(leaderId);
            mainWorker.setState(leaderFollower);
            return leaderFollower.AppendEntries(term,leaderId,prevLogIndex,prevLogTerm,entries,leaderCommit);
        }

        return currentTerm+";False";
    }

    public String RequestVote(long term, String candidateId, int lastLogIndex, int lastLogTerm) {
        //在candidate中,这里只能投自己,否则就要变成Follower
        if (term<currentTerm){
            return currentTerm + ";False";
        }else if(term>currentTerm){
            currentTerm = term;
            Follower leaderFollower = new Follower(candidateId);
            mainWorker.setState(leaderFollower);
            return leaderFollower.RequestVote(term,candidateId,lastLogIndex,lastLogTerm);
        }else if(candidateId.equals(selfID)) {//这里隐含了term == currentTerm的意思
            return currentTerm + ";True;";
        }else if(isNewer(lastLogIndex,lastLogTerm)){
            Follower leaderFollower = new Follower(candidateId);
            mainWorker.setState(leaderFollower);
            return leaderFollower.RequestVote(term,candidateId,lastLogIndex,lastLogTerm);
        }
        return currentTerm + ";False";
    }

    /*
    public void doVote() throws Exception{
        MethodCall call=new MethodCall(getClass().getMethod("RequestVote",
                long.class, String.class,int.class,int.class,byte[].class,long.class));
        //这里用random超时就可以实现了
        RequestOptions opts=new RequestOptions(ResponseMode.GET_ALL, 1000);
        call.setArgs(++currentTerm,selfID,lastLog.getIndex(),lastLog.getTerm());
        RspList rsp_list=mainWorker.GetRpcDispacher().callRemoteMethods(null, call, opts);
        Iterator iter = rsp_list.entrySet().iterator();
        int count = 0;
        while (iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            Rsp val = (Rsp)entry.getValue();
            String response = (String)val.getValue();
            if (response.split(";")[1].equals("True")){
                count +=1;
            }
        }
        synchronized (mainWorker){
            if (count>= clusterSize/2+1 && mainWorker.isCandidate()){
                Leader leader = new Leader();
                mainWorker.setState(leader);
            }
        }
    }
    */
    private class VoteTask implements TimerTask{
        public void run(Timeout timeout) throws Exception {
            MethodCall call=new MethodCall(getClass().getMethod("RequestVote",
                    long.class, String.class,int.class,int.class,byte[].class,long.class));
            //这里用random超时就可以实现了
            RequestOptions opts=new RequestOptions(ResponseMode.GET_ALL, 1000);
            call.setArgs(++currentTerm,selfID,lastLog.getIndex(),lastLog.getTerm());
            RspList rsp_list=mainWorker.GetRpcDispacher().callRemoteMethods(null, call, opts);
            Iterator iter = rsp_list.entrySet().iterator();
            int count = 0;
            while (iter.hasNext()){
                Map.Entry entry = (Map.Entry) iter.next();
                Rsp val = (Rsp)entry.getValue();
                String response = (String)val.getValue();
                if (response.split(";")[1].equals("True")){
                    count +=1;
                }
            }
            synchronized (mainWorker){
                if (count>= clusterSize/2+1 && mainWorker.isCandidate()){
                    Leader leader = new Leader();
                    mainWorker.setState(leader);
                }else{
                    Random random = new Random();
                    //这里随机超时
                    int randomTimeout = (random.nextInt(5)+3)*100;
                    hashedWheelTimer.newTimeout(new VoteTask(),randomTimeout, TimeUnit.MILLISECONDS);
                }
            }
        }
    }
}
