package EasyRaft.worker;

import EasyRaft.StateManager;
import EasyRaft.ServerIO.NetworkIO;
import EasyRaft.ServerIO.RaftKeeper;

/**
 * Created by jrj on 17-10-30.
 */
public class MainWorker{
    public static void main(String[] args){
        try {
            StateManager stateManager = new StateManager();
            RaftKeeper.setStateManager(stateManager);
            new NetworkIO(30304);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
