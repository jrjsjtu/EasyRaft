package EasyRaft.requests;

import EasyRaft.client.RaftClient;

/**
 * Created by jrj on 18-1-27.
 */
public class SetSlotRequest extends AbstractRequest {
    int idx,requestIdx;
    String selfAddress;
    public SetSlotRequest(int requestIdx,int idx,String selfAddress){
        super();
        this.idx = idx;
        this.requestIdx = requestIdx;
        this.selfAddress = selfAddress;
    }

    public void processResult(String result) {

    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(RaftClient.SetSlotRequest);
        stringBuilder.append(requestIdx).append('|');
        stringBuilder.append(idx).append('|');
        stringBuilder.append(selfAddress);
        return stringBuilder.toString();
    }
}
