package state;

import Utils.Timeout;
import Utils.TimerTask;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.*;
import worker.MainWorker;
import org.jgroups.util.UUID;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-10-30.
 */

public class Leader extends State {
    Timeout timeoutForPeriodic;
    HashMap<String,MatchInfo> hashMap;
    LinkedBlockingQueue<UUID> followersNotConsistent;
    public Leader() {
        //timeoutForLeader = hashedWheelTimer.newTimeout(new LeaderTimerTask(),1000, TimeUnit.MILLISECONDS);
        System.out.println("become Leader!!!");
        hashMap = new HashMap<String, MatchInfo>();
        jChannel.getView();
        timeoutForPeriodic = hashedWheelTimer.newTimeout(new AppendTask(),0, TimeUnit.MILLISECONDS);
        followersNotConsistent = new LinkedBlockingQueue<UUID>();
    }

    @Override
    public String AppendEntries(long term, String leaderId, long prevLogIndex, long prevLogTerm, byte[] entries, long leaderCommit) {
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
    public String RequestVote(long term, String candidateId, long lastLogIndex, long lastLogTerm) {
        if (term > currentTerm){
            currentTerm = term;
            Follower follower = new Follower(candidateId);
            mainWorker.setState(follower);
            return follower.RequestVote(term,candidateId,lastLogIndex,lastLogTerm);
        }
        return currentTerm + ";False";
    }

    private class MatchInfo{
        private long nextIndex;
        private long matchIndex;
        MatchInfo(long nextIndex,long matchIndex){
            this.nextIndex = nextIndex;
            this.matchIndex = matchIndex;
        }

        public long getNextIndex(){return nextIndex;}
        public long getMatchIndex(){return matchIndex;}
    }
    private class AppendTask implements TimerTask{
        public void run(Timeout timeout) throws Exception {
            synchronized (mainWorker){
                if (!mainWorker.isLeader()){
                    return;
                }
            }
            MethodCall call=new MethodCall(MainWorker.class.getMethod("AppendEntries",
                    long.class, String.class,long.class,long.class,byte[].class,long.class));
            //这里用random超时就可以实现了
            RequestOptions opts=new RequestOptions(ResponseMode.GET_ALL, 400);
            call.setArgs(currentTerm,selfID,lastLog.getIndex(),lastLog.getTerm(),null,commitIndex);
            RspList rsp_list=mainWorker.GetRpcDispacher().callRemoteMethods(null, call, opts);
            Iterator iter = rsp_list.entrySet().iterator();
            while (iter.hasNext()){
                Map.Entry entry = (Map.Entry) iter.next();
                Rsp val = (Rsp)entry.getValue();
                String response = (String)val.getValue();
                if (response != null && response.split(";")[1].equals("False")){
                    UUID uuid = (UUID)entry.getKey();
                    if (!followersNotConsistent.contains(uuid)){
                        followersNotConsistent.put(uuid);
                        new Thread(new ConsistentLog(uuid,lastLog.getIndex(),lastLog.getTerm())).start();
                    }
                }
            }
            synchronized (mainWorker){
                if (mainWorker.isLeader()){
                    hashedWheelTimer.newTimeout(new AppendTask(),500, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private class ConsistentLog implements Runnable{
        // if we keep consistency only through heartBeat then the speed may be very slow.
        UUID uuid;long lastLogIndex;long lastLogTerm;
        ConsistentLog(UUID uuid,long lastLogIndex,long lastLogTerm){
            this.lastLogIndex = lastLogIndex;
            this.lastLogTerm = lastLogTerm;
            this.uuid = uuid;
        }
        public void run() {
            try {
                MethodCall call = new MethodCall(MainWorker.class.getMethod("AppendEntries",
                        long.class, String.class, long.class, long.class, byte[].class, long.class));
                //这里用random超时就可以实现了
                RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 1000);
                while (true){
                    call.setArgs(currentTerm, selfID, --lastLogIndex, --lastLogTerm, null, commitIndex);
                    RspList rsp_list = mainWorker.GetRpcDispacher().callRemoteMethods(null, call, opts);
                    if (rsp_list.getFirst()!=null){
                        String resultStr = (String)rsp_list.getFirst();
                        if (resultStr.split(";")[1].equals("True")) {
                            break;
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
