package state;

import Utils.HashedWheelTimer;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Created by jrj on 17-10-30.
 */
public abstract class State {
    protected static JChannel jChannel;
    protected static MainWorker mainWorker;
    protected static int FOLLOWER = 0;
    protected static int CANDIDATE = 1;
    protected static int LEADER = 2;
    protected static final int clusterSize = 3;

    protected static String selfID;
    protected static long term = 0;
    protected static HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();

    protected static LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue();
    protected static AtomicInteger curState;
    public static void setJChannel(JChannel jChannel){
        State.selfID = jChannel.getAddress().toString();
        State.jChannel = jChannel;
    }

    public static void setMainWorker(MainWorker mainWorker){
        State.mainWorker = mainWorker;
    }
    public abstract void fireWhenViewAccepted(View new_view,MainWorker mainWorker);
    public abstract void fireWhenRaftMessageReceived(RaftMessage raftMessage);

    public void fireWhenMessageReceived(Message msg,MainWorker mainWorker){
        RaftMessage raftMessage = new RaftMessage(msg);
        if (raftMessage.getSender() != selfID){
            fireWhenRaftMessageReceived(raftMessage);
        }
    }
}
