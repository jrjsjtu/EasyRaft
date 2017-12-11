package EasyRaft;

import state.Follower;
import state.State;

import java.util.concurrent.DelayQueue;

/**
 * Created by jrj on 17-12-11.
 */
public class StateAdmin {
    private DelayQueue delayQueue;
    private Thread workerThread;
    private Thread delayQueuePoller;
    private State current;
    private long currentTerm;
    private String voteFor;
    private String[] logs;
    StateAdmin(){
        delayQueue = new DelayQueue();
        current = new Follower();

    }
}
