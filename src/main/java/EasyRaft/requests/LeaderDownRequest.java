package EasyRaft.requests;

/**
 * Created by jrj on 18-1-28.
 */
public class LeaderDownRequest extends AbstractRequest {
    int epoch;

    public int getEpoch() {
        return epoch;
    }

    public LeaderDownRequest(int epoch){
        this.epoch = epoch;
    }

    public void processResult(String result) {

    }
}
