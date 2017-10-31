package state;

import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

/**
 * Created by jrj on 17-10-30.
 */
public class Candidate extends State {
    public Candidate(){
        curState = CANDIDATE;
    }

    public void fireWhenViewAccepted(View new_view, MainWorker mainWorker) {

    }

    public void fireWhenMessageReceived(Message msg, MainWorker mainWorker) {

    }

}
