package state;

import Utils.Timeout;
import Utils.TimerTask;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.RspList;
import worker.MainWorker;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-10-30.
 */

public class Leader extends State {
    Timeout timeoutForLeader,timeoutForPeriodic;
    String leader;

    public Leader() {
        //timeoutForLeader = hashedWheelTimer.newTimeout(new LeaderTimerTask(),1000, TimeUnit.MILLISECONDS);
        System.out.println("become Leader!!!Ha Ha Ha");
        timeoutForPeriodic = hashedWheelTimer.newTimeout(new AppendTask(),0, TimeUnit.MILLISECONDS);
    }

    @Override
    public String AppendEntries(long term, String leaderId, int prevLogIndex, int prevLogTerm, byte[] entries, long leaderCommit) {
        if (term > currentTerm){
            currentTerm = term;
            Follower follower = new Follower(leaderId);
            mainWorker.setState(follower);
            return follower.AppendEntries(term,leaderId,prevLogIndex,prevLogTerm,entries,leaderCommit);
        }
        if (term == currentTerm && leaderId.equals(selfID)){
            return currentTerm + ";True";
        }
        return currentTerm + ";False";
    }

    @Override
    public String RequestVote(long term, String candidateId, int lastLogIndex, int lastLogTerm) {
        if (term > currentTerm){
            currentTerm = term;
            Follower follower = new Follower(candidateId);
            mainWorker.setState(follower);
            return follower.RequestVote(term,candidateId,lastLogIndex,lastLogTerm);
        }
        return currentTerm + ";False";
    }

    private class AppendTask implements TimerTask{
        public void run(Timeout timeout) throws Exception {
            MethodCall call=new MethodCall(getClass().getMethod("AppendEntries",
                    long.class, String.class,int.class,int.class,byte[].class,long.class));
            //这里用random超时就可以实现了
            RequestOptions opts=new RequestOptions(ResponseMode.GET_ALL, 1000);
            call.setArgs(currentTerm,selfID,lastLog.getIndex(),lastLog.getTerm(),null,commitIndex);
            RspList rsp_list=mainWorker.GetRpcDispacher().callRemoteMethods(null, call, opts);
            synchronized (mainWorker){
                if (mainWorker.isLeader()){
                    hashedWheelTimer.newTimeout(new AppendTask(),500, TimeUnit.MILLISECONDS);
                }
            }
        }
    }
}
