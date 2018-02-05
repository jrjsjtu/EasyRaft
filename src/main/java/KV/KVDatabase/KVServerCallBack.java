package KV.KVDatabase;

import EasyRaft.client.callBack.RaftCallBack;
import EasyRaft.client.callBack.RaftClientImp;

import java.util.ArrayList;

/**
 * Created by jrj on 18-1-27.
 */
public class KVServerCallBack implements RaftCallBack {
    RaftClientImp raftClientImp;
    ArrayList<String> currentSlot,alive;
    public void onBecomeLeader(RaftClientImp raftClientImp) {
        this.raftClientImp = raftClientImp;
        currentSlot = raftClientImp.getCurrentSlot();
        alive = raftClientImp.getCurrentAlive();
        raftClientImp.joinCLuster(raftClientImp.getLeaderInfo());
        /*
        for(int i=0;i<currentSlot.size();i++){
            String tmp = currentSlot.get(i);
            if (!tmp.equals("null") && !alive.contains(tmp)){
                raftClientImp.leaveCLuster(tmp);
                raftClientImp.setSlot(i,"null");
                System.out.println("remove " + tmp  + " on becoming leader");
            }
        }
        */
    }

    public void onSelectLeaderFailed(RaftClientImp raftClientImp){
        System.out.println("fail callBack");
        raftClientImp.joinCLuster(raftClientImp.getLeaderInfo());
    }

    public void onLeaderFailed(int epoch) {
        raftClientImp.electLeader(epoch+1);
    }

    //address 应该是 ip+port+sharding
    public void onMemberJoinWhenLeader(int idx,String address) {
        raftClientImp.setSlot(idx,address);
        for(int i=currentSlot.size();i<idx+1;i++){
            currentSlot.add("null");
        }
        currentSlot.set(idx,address);
    }

    public void onMemberLeaveWhenLeader(String address) {
        int idx = -1;
        for (int i=0;i<currentSlot.size();i++){
            if (currentSlot.get(i).equals(address)){
                idx = i;
                break;
            }
        }
        if (idx != -1){
            raftClientImp.leaveCLuster(address);
            raftClientImp.setSlot(idx,"null");
        }
    }

}
