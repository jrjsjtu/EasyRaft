package EasyRaft.requests;

import EasyRaft.client.RaftClient;

import java.util.ArrayList;

/**
 * Created by jrj on 18-1-28.
 */
public class QuerySlotRequest extends AbstractRequest {
    int requestIdx;
    private ArrayList<String> result;

    public QuerySlotRequest(int requestIdx){
        super();
        this.requestIdx = requestIdx;
    }

    public ArrayList<String> getResult() {
        return result;
    }

    public void setResult(ArrayList<String> result) {
        this.result = result;
    }

    public void processResult(String result) {

    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(RaftClient.QuerySlotRequest).append(requestIdx);
        return stringBuilder.toString();
    }
}
