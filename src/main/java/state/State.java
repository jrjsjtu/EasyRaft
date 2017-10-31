package state;

import Utils.HashedWheelTimer;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;
/**
 * Created by jrj on 17-10-30.
 */
public abstract class State {
    protected static JChannel jChannel;
    protected static int FOLLOWER = 0;
    protected static int CANDIDATE = 1;
    protected static int LEADER = 2;
    protected static long term = 0;
    protected static HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();
    protected int curState;
    public static void setjChannel(JChannel jChannel){
        State.jChannel = jChannel;
    }
    public abstract void fireWhenViewAccepted(View new_view,MainWorker mainWorker);
    public abstract void fireWhenMessageReceived(Message msg,MainWorker mainWorker);
}
