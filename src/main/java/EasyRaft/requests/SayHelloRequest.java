package EasyRaft.requests;

import EasyRaft.client.RaftClient;

/**
 * Created by jrj on 18-1-31.
 */
public class SayHelloRequest extends AbstractRequest {
    String appendInfo;
    public SayHelloRequest(String appendInfo){
        this.appendInfo = appendInfo;
    }
    public void processResult(String result) {

    }
    @Override
    public String toString(){
        return RaftClient.SayHelloRequest + appendInfo;
    }
}
