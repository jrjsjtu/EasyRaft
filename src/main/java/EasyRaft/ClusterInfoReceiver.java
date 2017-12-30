package EasyRaft;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import state.Leader;
import state.State;

import java.nio.charset.Charset;

/**
 * Created by jrj on 17-12-20.
 */
public class ClusterInfoReceiver extends ReceiverAdapter {
    //这里就相当于raft的客户端了,尽量少地侵入raft协议
    StateManager stateManager;
    public ClusterInfoReceiver(StateManager stateManager){
        this.stateManager = stateManager;
    }
    @Override
    public void receive(Message msg) {
        String line = msg.getSrc().toString();
        System.out.println(line);
    }

    @Override
    public void viewAccepted(final View view) {

    }


}
