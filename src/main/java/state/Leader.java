package state;

import Utils.Timeout;
import Utils.TimerTask;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-10-30.
 */

public class Leader extends State {
    Timeout timeoutForLeader,timeoutForPeriodic;
    String leader;

    public Leader() {
        timeoutForLeader = hashedWheelTimer.newTimeout(new LeaderTimerTask(),1000, TimeUnit.MILLISECONDS);
        timeoutForPeriodic = hashedWheelTimer.newTimeout(new PeriodicTask(),1000, TimeUnit.MILLISECONDS);
        System.out.println("become Leader!!!Ha Ha Ha");
    }

    public void fireWhenViewAccepted(View new_view, MainWorker mainWorker) {

    }

    public void fireWhenRaftMessageReceived(RaftMessage raftMessage) {
        try {
            linkedBlockingQueue.put(raftMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private class PeriodicTask implements TimerTask{
        public void run(Timeout timeout) throws Exception {
            Message message = new Message(null,jChannel.getAddress(),RaftMessage.LeaderHeartBeat+";"+selfID+";LeaderHeartBeat;"+ (++term));
            jChannel.send(message);
            timeoutForPeriodic = hashedWheelTimer.newTimeout(new PeriodicTask(),1000, TimeUnit.MILLISECONDS);
        }
    }
    private class LeaderTimerTask implements TimerTask {
        public void run(Timeout timeout) throws Exception {
            while (!linkedBlockingQueue.isEmpty()){
                RaftMessage raftMessage;
                raftMessage = (RaftMessage) linkedBlockingQueue.take();
                switch (raftMessage.getMessageType()){
                    case RaftMessage.LeaderHeartBeat:
                        if (raftMessage.getTerm()>=term) {
                            term = raftMessage.getTerm();
                            leader = raftMessage.getSender();
                            mainWorker.setState(new LeaderFollower(leader));
                            timeoutForPeriodic.cancel();
                            return;
                        }
                }
            }
            timeoutForLeader = hashedWheelTimer.newTimeout(new LeaderTimerTask(),1000, TimeUnit.MILLISECONDS);
        }
    }
}
