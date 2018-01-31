package EasyRaft.requests;

import EasyRaft.client.RaftClient;

/**
 * Created by jrj on 18-1-29.
 */
public class JoinClusterRequest extends AbstractRequest{
    String append;
    int requestIdx;
    public JoinClusterRequest(int requestIdx,String append){
        super();
        this.append = append;
        this.requestIdx = requestIdx;
    }
    public void processResult(String result) {

    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(RaftClient.JoinClusterRequest);
        stringBuilder.append(requestIdx).append('|');
        stringBuilder.append(append);
        return stringBuilder.toString();
    }
}
