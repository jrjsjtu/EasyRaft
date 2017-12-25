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
    HashMap<Address,Long> matchIndex;

    HashMap<Address,Long> notISRMap;
    public Leader() {
        System.out.println("become Leader!!!");
        //For Test
        stringBuilder = new StringBuilder();

        matchIndex = new HashMap<Address, Long>();
        notISRMap = new HashMap<Address, Long>(); //就是论文里的nextIndex

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
        System.out.println("leader " + term + "  " + currentTerm);
        if (term > currentTerm){
            currentTerm = term;
            Follower follower = new Follower(candidateId);
            stateManager.setState(follower);
            return follower.RequestVote(term,candidateId,lastLogIndex,lastLogTerm);
        }
        return currentTerm + ";False";
    }

    StringBuilder stringBuilder;
    public void processAppendRpcResult(RspList rspList){
        try{
            Iterator iter = rspList.entrySet().iterator();
            int count  = 0;
            long remoteIndex=0,lastRpcIndex=0;
            while (iter.hasNext()){
                Map.Entry entry = (Map.Entry) iter.next();
                Rsp val = (Rsp)entry.getValue();
                String response = (String)val.getValue();
                if (response == null || response.equals("self rpc")){
                    continue;
                }
                UUID uuid = (UUID)entry.getKey();
                String[] resultList = response.split(";");
                //如果接收到的 RPC 请求或响应中，任期号T > currentTerm，那么就令 currentTerm 等于 T，并切换状态为跟随者
                if(Long.parseLong(resultList[0])>currentTerm){
                    Follower follower = new Follower(uuid.toString());
                    stateManager.setState(follower);
                    return;
                }
                //这里与论文中的实现有所不同,在返回值中加入了,调用rpc后的lastLog的index,方便程序的实现
                remoteIndex = Long.parseLong(resultList[2]);
                lastRpcIndex = Long.parseLong(resultList[3]);
                if (resultList[1].equals("False")){
                    //可能出现rpc的返回结果乱序的问题.
                    updateNextIndexWhenFalse(uuid,lastRpcIndex);
                }else{
                    updateNextIndexWhenTrue(uuid,remoteIndex);
                    count ++;
                }
            }
            if (count>=clusterSize/2){
                checkMatchIndexAndCommitForMajority(remoteIndex);
            }else{
                checkMatchIndexAndCommitForMinor(remoteIndex);
            }
            commitIndex = lastApplied;
            stateManager.submitDelayed(new HeartBeatSendTask(this,System.currentTimeMillis()+100));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void checkMatchIndexAndCommitForMajority(long remoteIndex){
        for(long i = lastApplied+1;i<=remoteIndex;i++){
            System.out.println("now execute " + i);
        }
        if (remoteIndex>lastApplied){
            lastApplied = remoteIndex;
        }
    }

    private void checkMatchIndexAndCommitForMinor(long remoteIndex){
        long tmpIndex = lastApplied;
        while (tmpIndex<remoteIndex){
            tmpIndex++;
            Set<Map.Entry<Address,Long>> notISRSet = matchIndex.entrySet();
            int count = 0;
            for (Map.Entry<Address,Long> entry:notISRSet){
                if (tmpIndex<entry.getValue()){
                    count ++;
                }
            }
            if (count>clusterSize/2){
                System.out.println("now execute " + tmpIndex);
                lastApplied = tmpIndex;
            }else{
                return;
            }
        }
    }

    private void updateNextIndexWhenTrue(UUID uuid,long remoteIndex){
        if (notISRMap.containsKey(uuid)){
            if (remoteIndex==lastLog.getIndex()){
                System.out.println("remove notISRMap " + uuid.toString());
                notISRMap.remove(uuid);
            }else{
                notISRMap.put(uuid,remoteIndex);
            }
        }
        if (matchIndex.containsKey(uuid)){
            long tmp = matchIndex.get(uuid);
            //防止有乱序的rpc结果才这么写
            if (remoteIndex>tmp){
                matchIndex.put(uuid,tmp);
            }
        }else{
            matchIndex.put(uuid,remoteIndex);
        }
    }

    private void updateNextIndexWhenFalse(UUID uuid,long lastRpcIndex){
        long nextIndex = lastRpcIndex;
        long lastTerm = logs.get((int)nextIndex).getTerm();
        //可能出现rpc的返回结果乱序的问题.
        if (notISRMap.containsKey(uuid)){
            long tmp = notISRMap.get(uuid);
            if (tmp<nextIndex){
                //之前的一个rpc结果现在才返回,这个nextIndex就是调用rpc时传入的prevLogIndex参数
                return;
            }
        }
        //直接找上一个term的消息
        while((--nextIndex)>=0){
            long tmp = logs.get((int)nextIndex).getTerm();
            if (tmp<lastTerm){
                System.out.println("insert into notISRMap " + uuid.toString());
                notISRMap.put(uuid,tmp);
                return;
            }
        }
    }

    public class HeartBeatSendTask extends RaftDelayedTask {
        HeartBeatSendTask(State state,long time){
            super(state,time);
        }

        public void run() {
            try{
                MethodCall call=new MethodCall(StateManager.class.getMethod("AppendEntries",
                        long.class, String.class,long.class,long.class,byte[].class,long.class,String.class));
                //这里用random超时就可以实现了
                RequestOptions opts=new RequestOptions(ResponseMode.GET_ALL, 200);

                if (stringBuilder.length()==0){
                    Random random = new Random();
                    if (random.nextInt(20)==0){
                        //System.out.println("add new Entry");
                        stringBuilder.append(currentTerm + ","+(lastLog.getIndex()+1)+",Extra log");
                    }
                    call.setArgs(currentTerm,selfID,lastLog.getIndex(),lastLog.getTerm(),null,commitIndex,"all");
                }else{
                    call.setArgs(currentTerm,selfID,lastLog.getIndex(),lastLog.getTerm(),stringBuilder.toString().getBytes(),commitIndex,"all");
                    insertEntriesIntoLogs(stringBuilder.toString().getBytes());
                    stringBuilder = new StringBuilder();
                }
                stateManager.submitIO(new HeartBeatIOTask(opts,call,null));

                Set<Map.Entry<Address, Long>> entryseSet=notISRMap.entrySet();
                for (Map.Entry<Address, Long> entry:entryseSet) {
                    MethodCall call1=new MethodCall(StateManager.class.getMethod("AppendEntries",
                            long.class, String.class,long.class,long.class,byte[].class,long.class,String.class));

                    RequestOptions opts1=new RequestOptions(ResponseMode.GET_ALL, 200);
                    //这个value存的是下一次prevlog的index,所以get的byte是不包括这一条的.
                    RaftLog raftLog = state.getLog(entry.getValue());
                    byte[] byteForFollower = State.getByteForFollower(raftLog.getIndex());
                    System.out.println("log array size is " + logs.size());
                    call.setArgs(currentTerm,selfID,raftLog.getIndex(),raftLog.getTerm(),byteForFollower,commitIndex,entry.getKey().toString());
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
                    //就算在rpc中指明了receiver还是以广播的形式在调用.
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
