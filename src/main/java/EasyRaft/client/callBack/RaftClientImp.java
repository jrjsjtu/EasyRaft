package EasyRaft.client.callBack;

import java.util.ArrayList;

/**
 * Created by jrj on 18-1-22.
 */
public interface RaftClientImp {
    void electLeader();
    void electLeader(int epoch);
    void setSlot(int idx,String ADDRESS);
    void joinCLuster(String info);
    void leaveCLuster(String info);

    ArrayList<String> getCurrentSlot();
    ArrayList<String> getCurrentAlive();
}
