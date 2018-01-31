package EasyRaft.requests;

import EasyRaft.client.RaftClient;

/**
 * Created by jrj on 18-1-27.
 */
public class SelectLeaderRequest extends AbstractRequest {
    int epoch,requestId;
    String selfID;
    RaftClient raftClient;

    public SelectLeaderRequest(int requestId,int epoch, String selfID, RaftClient raftClient){
        super();
        this.requestId = requestId;
        this.epoch = epoch;
        this.selfID = selfID;
        this.raftClient = raftClient;
    }

    public void setEpoch(int epoch){
        this.epoch = epoch;
    }
    //总是type + request id
    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(RaftClient.SelectLeaderRequest);
        stringBuilder.append(requestId).append('|');
        stringBuilder.append(epoch).append('|');
        stringBuilder.append(selfID);
        return stringBuilder.toString();
    }

    public void processResult(String result) {

    }
}
