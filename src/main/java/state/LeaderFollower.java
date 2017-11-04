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
    Timeout timeoutForFollower,timeoutForPeriodic;
    String leader;

    public LeaderFollower(String leaderAddress) {
        super(leaderAddress);
        timeoutForFollower = hashedWheelTimer.newTimeout(new LeaderFollowerTimerTask(),1000, TimeUnit.MILLISECONDS);
        timeoutForPeriodic = hashedWheelTimer.newTimeout(new PeriodicTask(),5000, TimeUnit.MILLISECONDS);
        receivedHeartbeat = false;
        System.out.println("become leaderFollower");
    }

    public void fireWhenRaftMessageReceived(RaftMessage raftMessage) {
        try {
            linkedBlockingQueue.put(raftMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class LeaderFollowerTimerTask implements TimerTask {
        public void run(Timeout timeout) throws Exception {
            while (!linkedBlockingQueue.isEmpty()){
                RaftMessage raftMessage;
                raftMessage = (RaftMessage) linkedBlockingQueue.take();
                switch (raftMessage.getMessageType()){
                    case RaftMessage.LeaderHeartBeat:
                        receivedHeartbeat = true;
                        if (raftMessage.getTerm()>term) {
                            term = raftMessage.getTerm();
                            leader = raftMessage.getSender();
                        }
                        return;
                }
            }
            timeoutForFollower = hashedWheelTimer.newTimeout(new LeaderFollowerTimerTask(),1000, TimeUnit.MILLISECONDS);
        }
    }
}

