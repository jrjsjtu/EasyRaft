package state;

import Utils.Timeout;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import Utils.Timeout;
import Utils.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.Random;
/**
 * Created by jrj on 17-10-30.
 */
public abstract class Follower extends State {
    protected String leaderAddress;
    protected Timeout timeoutForFollower,timeoutForPeriodic;
    protected boolean receivedHeartbeat;
    public Follower(String leaderAddress){
        this.leaderAddress = leaderAddress;
        //curState.set(FOLLOWER);
    }

    protected class PeriodicTask implements TimerTask{
        public void run(Timeout timeout) throws Exception {
            if (!receivedHeartbeat){
                mainWorker.setState(new Candidate());
                timeoutForFollower.cancel();
            }else{
                receivedHeartbeat = false;
                timeoutForPeriodic = hashedWheelTimer.newTimeout(new PeriodicTask(),5000, TimeUnit.MILLISECONDS);
            }
        }
    }
    public void joinGroup(){

    }

    public void fireWhenViewAccepted(View new_view, MainWorker mainWorker) {

    }
}
