package state;

import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * Created by jrj on 17-10-30.
 */

public class Leader extends State {
    public Leader(){
        curState = LEADER;
        WeakHashMap hashMap = new WeakHashMap();
        hashMap.clear();
    }

    public void fireWhenViewAccepted(View new_view, MainWorker mainWorker) {

    }

    public void fireWhenMessageReceived(Message msg, MainWorker mainWorker) {

    }
}
