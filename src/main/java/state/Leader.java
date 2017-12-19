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
    HashMap<Address,Long> matchIndex,nextIndex;

    HashMap<Address,Long> notISRMap;
    public Leader() {
        System.out.println("become Leader!!!");
        //For Test
        stringBuilder = new StringBuilder();
        stringBuilder.append("First log");

        matchIndex = new HashMap<Address, Long>();
        notISRMap = new HashMap<Address, Long>();
        nextIndex = new HashMap<Address, Long>();

        submitAppendTask();
    }

    public void submitAppendTask(){
        new HeartBeatSendTask(this,System.currentTimeMillis()).run();
    }

    public String AppendEntries(final long term,final String leaderId,final long prevLogIndex,final long prevLogTerm,final byte[] entries,final long leaderCommit) {
        if (leaderId.equals(selfID)){
            return "self rpc";
        }
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

    String lastLogForRpc = null;
    StringBuilder stringBuilder;
    public void processAppendRpcResult(RspList rspList){
        try{
            Iterator iter = rspList.entrySet().iterator();
            while (iter.hasNext()){
                System.out.println("process !");
                Map.Entry entry = (Map.Entry) iter.next();
                Rsp val = (Rsp)entry.getValue();
                String response = (String)val.getValue();
                if (response == null || response.equals("self rpc")){
                    continue;
                }
                UUID uuid = (UUID)entry.getKey();
                String[] resultList = response.split(";");
                //遇到term较大的就转为follower,用于脑裂时的恢复
                if(Long.parseLong(resultList[0])>currentTerm){
                    Follower follower = new Follower(uuid.toString());
                    stateManager.setState(follower);
                    return;
                }
                //这里与论文中的实现有所不同,在返回值中加入了,调用rpc时的index值,方便程序的阅读性
                long remoteIndex = Long.parseLong(resultList[2]);
                if (resultList[1].equals("False")){
                    notISRMap.put(uuid,remoteIndex-1);
                }else{
                    if (remoteIndex==lastLog.getIndex() && notISRMap.containsKey(uuid)){
                        notISRMap.remove(uuid);
                    }
                }
            }
            stateManager.submitDelayed(new HeartBeatSendTask(this,System.currentTimeMillis()+100));
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

                if (stringBuilder.length()==0){
                    call.setArgs(currentTerm,selfID,lastLog.getIndex(),lastLog.getTerm(),null,commitIndex);
                }else{
                    call.setArgs(currentTerm,selfID,lastLog.getIndex(),lastLog.getTerm(),stringBuilder.toString().getBytes(),commitIndex);
                    insertEntriesIntoLogs(stringBuilder.toString().getBytes());
                    stringBuilder = new StringBuilder();
                }
                stateManager.submitIO(new HeartBeatIOTask(opts,call,null));

                Set<Map.Entry<Address, Long>> entryseSet=notISRMap.entrySet();
                for (Map.Entry<Address, Long> entry:entryseSet) {
                    MethodCall call1=new MethodCall(StateManager.class.getMethod("AppendEntries",
                            long.class, String.class,long.class,long.class,byte[].class,long.class));

                    RequestOptions opts1=new RequestOptions(ResponseMode.GET_ALL, 5000);
                    //这个value存的是下一次prevlog的index,所以get的byte是不包括这一条的.
                    RaftLog raftLog = state.getLog(entry.getValue());
                    byte[] byteForFollower = State.getByteForFollower(raftLog.getIndex());
                    call.setArgs(currentTerm,selfID,raftLog.getIndex(),raftLog.getTerm(),byteForFollower,commitIndex);
                    ArrayList<Address> arrayList = new ArrayList();
                    arrayList.add(entry.getKey());
                    stateManager.submitIO(new HeartBeatIOTask(opts1,call1, arrayList));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        private class HeartBeatIOTask implements Runnable{
            RequestOptions opts;MethodCall call;ArrayList<Address> dests;
            HeartBeatIOTask(RequestOptions opts,MethodCall call,ArrayList<Address> dests){
                this.opts = opts;this.call = call;this.dests = dests;
            }
            public void run() {
                RspList rsp_list= null;
                try {
                    List arrayList = stateManager.getMemberList();
                    rsp_list = stateManager.getRpcDispatcher().callRemoteMethods(dests, call, opts);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                AppendRpcResult appendRpcResult = new AppendRpcResult(state,rsp_list);
                stateManager.submitRpcResult(appendRpcResult);
            }
        }
    }
}
