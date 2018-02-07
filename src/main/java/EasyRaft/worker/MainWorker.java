package EasyRaft.worker;

import EasyRaft.StateManager;
import EasyRaft.ServerIO.NetworkIO;
import EasyRaft.ServerIO.RaftKeeper;
import Utils.RaftLogger;

import java.io.FileNotFoundException;

/**
 * Created by jrj on 17-10-30.
 */
public class MainWorker{
    public static void main(String[] args){
        try {
            RaftConfig raftConfig = null;
            try{
                raftConfig = new RaftConfig(args);
            }catch (Exception e){
                RaftLogger.log.info("Error occur during raftConfig initialization " + e.fillInStackTrace());
                System.exit(0);
            }
            StateManager stateManager = new StateManager();
            //RaftKeeper.setLeaderPort(xmlReader.getLeaderPort());
            RaftKeeper.setLeaderPort(raftConfig.getPort());
            RaftKeeper.setStateManager(stateManager);
            RaftKeeper.initCheckThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
