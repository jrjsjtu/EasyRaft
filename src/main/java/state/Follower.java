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
    Timeout timeoutForCandidate;
    TimerTask timerTask;
    String leaderAddress;
    public Follower(String leaderAddress){
        this.leaderAddress = leaderAddress;
        curState.set(FOLLOWER);
        while (!linkedBlockingQueue.isEmpty()){
            try {
                RaftMessage raftMessage = (RaftMessage) linkedBlockingQueue.take();
                if (raftMessage.getTerm()>term){
                    term = raftMessage.getTerm();
                    this.leaderAddress = raftMessage.getSender();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class FollowerTimerTask implements TimerTask{
        public void run(Timeout timeout) throws Exception {

        }
    }
    public void joinGroup(){
        Message message = new Message(null,jChannel.getAddress(),"1;"+jChannel.getAddressAsString()+";Join in Group");
        timeoutForCandidate = hashedWheelTimer.newTimeout(timerTask,3000, TimeUnit.MILLISECONDS);
        try {
            jChannel.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fireWhenViewAccepted(View new_view, MainWorker mainWorker) {
        if (curState.get() == FOLLOWER && new_view.size()>= clusterSize/2+1){
            timeoutForCandidate = hashedWheelTimer.newTimeout(timerTask,0, TimeUnit.MILLISECONDS);
        }
    }
}
