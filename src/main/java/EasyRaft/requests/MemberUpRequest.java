package EasyRaft.requests;

/**
 * Created by jrj on 18-1-28.
 */
public class MemberUpRequest extends AbstractRequest {
    int idx;
    String address;

    public String getAddress() {
        return address;
    }
    public void setAddress(String address){
        this.address = address;
    }

    public int getIdx() {
        return idx;
    }

    public MemberUpRequest(int idx, String address){
        this.idx= idx;
        this.address = address;
    }
    public void processResult(String result) {


    }
}
