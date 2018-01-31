package EasyRaft.requests;

import EasyRaft.client.RaftClient;

/**
 * Created by jrj on 18-1-29.
 */
public class LeaveClusterRequest extends AbstractRequest {
    int requetIdx;
    String address;
    public LeaveClusterRequest(int requestIdx,String address){
        this.requetIdx =requestIdx;
        this.address = address;
    }
    public void processResult(String result) {

    }
    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(RaftClient.LeaveClusterRequest);
        stringBuilder.append(requetIdx).append('|');
        stringBuilder.append(address);
        return stringBuilder.toString();
    }
}
