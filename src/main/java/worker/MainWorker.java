package worker;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import state.Candidate;
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
        channel = new JChannel();
        channel.setReceiver(this);
        channel.connect("Cluster");

        State.setJChannel(channel);
        current = new Candidate();
        State.setMainWorker(this);
        ((Candidate)current).joinGroup();
    }

    public void setState(State state){
        current = state;
    }

    @Override
    public void viewAccepted(View new_view) {

    }

    @Override
    public void receive(Message msg) {
        current.fireWhenMessageReceived(msg,this);
    }

    public static void main(String[] args){
        try {
            MainWorker mainWorker = new MainWorker();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
