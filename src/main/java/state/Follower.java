package state;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

/**
 * Created by jrj on 17-10-30.
 */
public class Follower extends State {
    public Follower(){
        curState = FOLLOWER;
        Message message = new Message(null,jChannel.getAddress(),jChannel.getAddressAsString()+";Join in Group");
        try {
            jChannel.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fireWhenViewAccepted(View new_view, MainWorker mainWorker) {

    }

    public void fireWhenMessageReceived(Message msg, MainWorker mainWorker) {

    }
}
