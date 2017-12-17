package EasyRaft;

import org.jgroups.util.RspList;
import state.State;

/**
 * Created by jrj on 17-12-13.
 */
public class VoteRpcResult {
    State receiver;
    RspList rspList;
    public VoteRpcResult(State receiver, RspList rspList){
        this.receiver = receiver;
        this.rspList = rspList;
    }

    public State getReceiver() {
        return receiver;
    }

    public void setReceiver(State receiver) {
        this.receiver = receiver;
    }

    public RspList getRspList() {
        return rspList;
    }

    public void setRspList(RspList rspList) {
        this.rspList = rspList;
    }
}
