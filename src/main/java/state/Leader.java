package state;

import EasyRaft.AppendRpcResult;
import EasyRaft.RaftDelayedTask;
import EasyRaft.StateManager;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.*;
import worker.MainWorker;
import org.jgroups.util.UUID;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by jrj on 17-10-30.
 */

public class Leader extends State {
    LinkedBlockingQueue<UUID> followersNotConsistent;
    HashMap<String,Long> matchIndex,nextIndex;
    public Leader() {
        System.out.println("become Leader!!!");
        //For Test
        logs.put(0l,new ArrayList<String>());
        logs.get(0l).add("hahahaha");
        RaftLog newRaftLog = new RaftLog(currentTerm,0,"hahahaha".getBytes());
        newRaftLog.setPrevLog(lastLog);
        lastLog = newRaftLog;
        followersNotConsistent = new LinkedBlockingQueue<UUID>();
        matchIndex = new HashMap<String, Long>();
        nextIndex = new HashMap<String, Long>();
        submitAppendTask();
    }

    public void submitAppendTask(){
        new HeartBeatSendTask(this,System.currentTimeMillis()).run();
    }

    public String AppendEntries(final long term,final String leaderId,final long prevLogIndex,final long prevLogTerm,final byte[] entries,final long leaderCommit) {
        if (term > currentTerm){
            currentTerm = term;
            Follower follower = new Follower(leaderId);
            stateManager.setState(follower);
            return follower.AppendEntries(term,leaderId,prevLogIndex,prevLogTerm,entries,leaderCommit);
        }
        if (term == currentTerm && leaderId.equals(selfID)){
            return currentTerm + ";True";
        }
        return currentTerm + ";False";
    }

    public String RequestVote(final long term,final String candidateId,final long lastLogIndex,final long lastLogTerm) {
        System.out.println("leader" + term + "  " + currentTerm);
        if (term > currentTerm){
            currentTerm = term;
            Follower follower = new Follower(candidateId);
            stateManager.setState(follower);
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

    /*
    private class ProcessHeartBeatRpcResult implements Runnable{
        private RspList rspList;
        ProcessHeartBeatRpcResult(RspList rspList){
            this.rspList = rspList;
        }
        public void run() {
            try{
                if (!stateManager.isLeader()){
                    return;
                }
                Iterator iter = rspList.entrySet().iterator();
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
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    */
    public void processAppendRpcResult(RspList rspList){
        try{
            stateManager.submitDelayed(new HeartBeatSendTask(this,System.currentTimeMillis()+100));
            if (rspList==null){
                return;
            }
            Iterator iter = rspList.entrySet().iterator();
            while (iter.hasNext()){
                Map.Entry entry = (Map.Entry) iter.next();
                Rsp val = (Rsp)entry.getValue();
                String response = (String)val.getValue();
                System.out.println(response);
                if (response != null && response.split(";")[1].equals("False")){
                    UUID uuid = (UUID)entry.getKey();
                    if (!followersNotConsistent.contains(uuid)){
                        followersNotConsistent.put(uuid);
                       // new Thread(new ConsistentLog(uuid,lastLog.getIndex(),lastLog.getTerm())).start();
                    }
                }else{
                    UUID uuid = (UUID)entry.getKey();
                    System.out.println(uuid.toString());
                    System.out.println(selfID);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public class HeartBeatSendTask extends RaftDelayedTask {
        HeartBeatSendTask(State state,long time){
            super(state,time);
        }

        public void run() {
            try{
                MethodCall call=new MethodCall(StateManager.class.getMethod("AppendEntries",
                        long.class, String.class,long.class,long.class,byte[].class,long.class));
                //这里用random超时就可以实现了
                RequestOptions opts=new RequestOptions(ResponseMode.GET_ALL, 5000);
                call.setArgs(currentTerm,selfID,lastLog.getIndex(),lastLog.getTerm(),null,commitIndex);
                stateManager.submitIO(new HeartBeatIOTask(opts,call));
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        private class HeartBeatIOTask implements Runnable{
            RequestOptions opts;MethodCall call;
            HeartBeatIOTask(RequestOptions opts,MethodCall call){
                this.opts = opts;this.call = call;
            }
            public void run() {
                RspList rsp_list= null;
                try {
                    List arrayList = stateManager.getMemberList();
                    if (arrayList.size()>=2){
                        System.out.println("error size > 2");
                    }
                    if (arrayList.size() != 0){
                        rsp_list = stateManager.getRpcDispatcher().callRemoteMethods(arrayList, call, opts);
                        System.out.println("result list size is " + rsp_list.size());
                    }else{
                        rsp_list = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                AppendRpcResult appendRpcResult = new AppendRpcResult(state,rsp_list);
                stateManager.submitRpcResult(appendRpcResult);
            }
        }
    }

    private class ConsistentLog implements Runnable{
        // if we keep consistency only through heartBeat then the speed may be very slow.
        UUID uuid;long lastLogIndex;long lastLogTerm;
        RaftLog raftLogLocal;
        ConsistentLog(UUID uuid,long lastLogIndex,long lastLogTerm){
            this.lastLogIndex = lastLogIndex;
            this.lastLogTerm = lastLogTerm;
            this.uuid = uuid;
            raftLogLocal = lastLog;
        }

        private void setIndexForNextRpc(){
            raftLogLocal = raftLogLocal.getPrev();
            if (raftLogLocal != null){
                lastLogIndex = raftLogLocal.getIndex();
                lastLogTerm = raftLogLocal.getTerm();
            }else{
                lastLogIndex = -1l;
                lastLogTerm = -1l;
            }
            // index and term begin from 0;
        }
        public void run() {
            try {
                MethodCall call = new MethodCall(StateManager.class.getMethod("AppendEntries",
                        long.class, String.class, long.class, long.class, byte[].class, long.class));
                RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 1000);
                Collection<Address> collection = new ArrayList();
                collection.add(uuid);
                while (true){
                    synchronized (stateManager){
                        if (!stateManager.isLeader()){
                            return;
                        }
                        setIndexForNextRpc();
                    }
                    System.out.println("For next heartBeat 2" + lastLogTerm + " " + lastLogIndex);
                    if (lastLogIndex == -1){
                        call.setArgs(currentTerm, selfID, -1, -1, "zero log".getBytes(), commitIndex);
                    }else{
                        call.setArgs(currentTerm, selfID, lastLogIndex, lastLogTerm, logs.get((int)lastLogTerm).get((int)lastLogIndex).getBytes(), commitIndex);
                    }
                    RspList rsp_list = stateManager.getRpcDispatcher().callRemoteMethods(collection, call, opts);
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
