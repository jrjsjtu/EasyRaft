package EasyRaft.requests;

/**
 * Created by jrj on 18-1-28.
 */
public class MemberDownRequest extends AbstractRequest {
    String address;

    public String getAddress() {
        return address;
    }

    public MemberDownRequest(String address){
        this.address = address;
    }
    public void processResult(String result) {

    }
}
