package EasyRaft.client.callBack;

import EasyRaft.client.RaftClient;

import java.util.ArrayList;

/**
 * Created by jrj on 18-1-27.
 */
public interface RaftCallBack {
    void onBecomeLeader(RaftClientImp raftClientImp);
    void onLeaderFailed(int epoch);

    void onMemberJoinWhenLeader(int idx,String address);
    void onMemberLeaveWhenLeader(String address);
}
