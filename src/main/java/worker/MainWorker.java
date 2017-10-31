package worker;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import state.Follower;
import state.State;

/**
 * Created by jrj on 17-10-30.
 */
public class MainWorker extends ReceiverAdapter {
    private static final int clusterSize = 3;
    private State current;
    JChannel channel;

    public MainWorker() throws Exception{
        current = new Follower();
        channel = new JChannel();
        channel.setReceiver(this);
        channel.connect("Cluster");
        State.setjChannel(channel);
    }

    public void setState(State state){
        current = state;
    }

    @Override
    public void viewAccepted(View new_view) {
        current.fireWhenViewAccepted(new_view,this);
    }

    @Override
    public void receive(Message msg) {
        current.fireWhenMessageReceived(msg,this);
    }
}
