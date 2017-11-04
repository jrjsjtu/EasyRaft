package state;

import Utils.Timeout;
import Utils.TimerTask;
import org.jgroups.Message;

import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-11-2.
 */
public class VoteFollower extends Follower {
    boolean receivedHeartbeat;
    String leader;

    public VoteFollower(String leaderAddress) {
        super(leaderAddress);
        timeoutForFollower = hashedWheelTimer.newTimeout(new LeaderFollowerTimerTask(),1000, TimeUnit.MILLISECONDS);
        timeoutForPeriodic = hashedWheelTimer.newTimeout(new PeriodicTask(),5000, TimeUnit.MILLISECONDS);
        receivedHeartbeat = false;
        System.out.println("become voteFollower");
    }
    /*
    private class PeriodicTask implements TimerTask{
        public void run(Timeout timeout) throws Exception {
            if (!receivedHeartbeat){
                mainWorker.setState(new Candidate());
                timeoutForFollower.cancel();
            }else{
                receivedHeartbeat = false;
                timeoutForPeriodic = hashedWheelTimer.newTimeout(new PeriodicTask(),1000, TimeUnit.MILLISECONDS);
            }
        }
    }
    */
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
                        term = raftMessage.getTerm();
                        leader = raftMessage.getSender();
                        timeoutForPeriodic.cancel();
                        mainWorker.setState(new LeaderFollower(leader));
                        return;
                    case RaftMessage.CandidateElection:
                        if (raftMessage.getTerm()>term){
                            receivedHeartbeat = true;
                            term = raftMessage.getTerm();
                            Message message = new Message(null,jChannel.getAddress(),RaftMessage.CandidateElection+";"+selfID+";"+raftMessage.getSender()+";"+ (term));
                            jChannel.send(message);
                        }
                }
            }
            timeoutForFollower = hashedWheelTimer.newTimeout(new LeaderFollowerTimerTask(),1000, TimeUnit.MILLISECONDS);
        }
    }
}
