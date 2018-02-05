package EasyRaft.requests;

import EasyRaft.client.RaftClient;

/**
 * Created by jrj on 18-1-27.
 */
public class SelectLeaderRequest extends AbstractRequest {
    int epoch,requestId;
    String selfID;
    boolean success = false;
    public SelectLeaderRequest(int requestId,int epoch, String selfID){
        super();
        this.requestId = requestId;
        this.epoch = epoch;
        this.selfID = selfID;
    }

    public void setEpoch(int epoch){
        this.epoch = epoch;
    }
    public int getEpoch(){return epoch;}

    public void setSuccess(){
        success = true;
    }
    public boolean isSuccess(){
        return success;
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
