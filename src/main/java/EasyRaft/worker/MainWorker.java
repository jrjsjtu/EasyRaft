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
            XMLReader xmlReader = new XMLReader("/home/jrj/Desktop/idea-IU-163.12024.16/java_learn/EasyRaft/src/main/java/EasyRaft/worker/config.xml");
            StateManager stateManager = new StateManager();
            //RaftKeeper.setLeaderPort(xmlReader.getLeaderPort());
            RaftKeeper.setLeaderPort(50002);
            RaftKeeper.setStateManager(stateManager);
            RaftKeeper.initCheckThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
