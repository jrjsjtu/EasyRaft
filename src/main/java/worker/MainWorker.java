package worker;

import EasyRaft.StateManager;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;
import state.Candidate;
import state.Follower;
import state.Leader;
import state.State;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by jrj on 17-10-30.
 */
public class MainWorker{
    public static void main(String[] args){
        try {
            StateManager stateManager = new StateManager();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
