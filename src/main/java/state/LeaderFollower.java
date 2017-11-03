package state;

import Utils.Timeout;
import Utils.TimerTask;
import org.jgroups.Message;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-11-2.
 */
public class LeaderFollower extends Follower {
    String leaderAddress;
    boolean receivedHeartbeat;
    LeaderFollowerTimerTask leaderFollowerTimerTask;
    public LeaderFollower(String leaderAddress) {
        super(leaderAddress);
        this.leaderAddress = leaderAddress;
        leaderFollowerTimerTask = new LeaderFollowerTimerTask();
    }

    public void fireAfterInitial() {
        while (!linkedBlockingQueue.isEmpty()) {
            try {
                RaftMessage raftMessage = (RaftMessage) linkedBlockingQueue.take();
                if (raftMessage.getMessageType() == RaftMessage.LeaderHeartBeat && raftMessage.getTerm() > term) {
                    this.leaderAddress = raftMessage.getSender();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        receivedHeartbeat = false;
        hashedWheelTimer.newTimeout(leaderFollowerTimerTask,5000,TimeUnit.MILLISECONDS);
    }

    public void fireWhenRaftMessageReceived(RaftMessage raftMessage) {
        Message message = new Message(null,jChannel.getAddress(),"2;"+selfID+";"+raftMessage.getSender()+";"+ (++term));
        try {
            jChannel.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class LeaderFollowerTimerTask implements TimerTask {
        public void run(Timeout timeout) throws Exception {
            if (!receivedHeartbeat){
                mainWorker.setState(new Candidate());
                return;
            }else{
                receivedHeartbeat = false;
            }
            hashedWheelTimer.newTimeout(leaderFollowerTimerTask,5000,TimeUnit.MILLISECONDS);
        }
    }
}
