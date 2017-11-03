package state;

import Utils.Timeout;
import Utils.TimerTask;
import org.jgroups.Message;

import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-11-2.
 */
public class VoteFollower extends Follower {
    String votedAddress;
    voteFollowerTimerTask voteFollowerTimerTask;
    boolean reveivedVote;
    public VoteFollower(String leaderAddress) {
        super(leaderAddress);
        this.votedAddress = leaderAddress;
        curState.set(FOLLOWER);
        voteFollowerTimerTask = new voteFollowerTimerTask();
    }

    public void fireAfterInitial() {
        while (!linkedBlockingQueue.isEmpty()) {
            try {
                RaftMessage raftMessage = (RaftMessage) linkedBlockingQueue.take();
                if (raftMessage.getTerm() > term && raftMessage.getMessageType() == RaftMessage.CandidateElection) {
                    term = raftMessage.getTerm();
                    this.leaderAddress = raftMessage.getSender();
                } else if (raftMessage.getMessageType() == RaftMessage.LeaderHeartBeat) {
                    mainWorker.setState(new LeaderFollower(raftMessage.getSender()));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void fireWhenRaftMessageReceived(RaftMessage raftMessage) {
        reveivedVote = true;
        if (raftMessage.getMessageType() == RaftMessage.CandidateElection){
            if (raftMessage.getTerm()>term){
                term = raftMessage.getTerm();
                Message message = new Message(null,jChannel.getAddress(),"2;"+selfID+";"+raftMessage.getSender()+";"+ term);
                try {
                    jChannel.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class voteFollowerTimerTask implements TimerTask {
        public void run(Timeout timeout) throws Exception {
            if (!reveivedVote){
                mainWorker.setState(new Candidate());
            }else{
                reveivedVote = false;
            }
            hashedWheelTimer.newTimeout(voteFollowerTimerTask,5000,TimeUnit.MILLISECONDS);
        }
    }
}
