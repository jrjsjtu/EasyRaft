package state;

import Utils.Timeout;
import Utils.TimerTask;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jrj on 17-10-30.
 */

public class Candidate extends State {
    String candidateAddress;
    long candidateTerm;
    long curMaxTerm;
    Timeout timeoutForCandidate;
    TimerTask timerTask;
    int voteForMe;
    public Candidate(){
        curState = new AtomicInteger(CANDIDATE);
        timerTask = new CandidateTimerTask();
        voteForMe ++;
    }

    private class CandidateTimerTask implements TimerTask{
        public void run(Timeout timeout) throws Exception {
            //这里用一个curstate,使得在状态改变时能够停止发送选举信息
            while (!linkedBlockingQueue.isEmpty()){
                RaftMessage raftMessage;
                raftMessage = (RaftMessage) linkedBlockingQueue.take();
                switch (raftMessage.getMessageType()){
                    case RaftMessage.CandidateElection:
                        if(raftMessage.getMessageType() > term){
                            term = raftMessage.getMessageType();
                            mainWorker.setState(new VoteFollower(raftMessage.getSender()));
                            return;
                        }else if(raftMessage.getMessageType() == term && raftMessage.getMessageContent() == selfID){
                            voteForMe++;
                        }
                        break;
                    case RaftMessage.LeaderHeartBeat:
                        term = raftMessage.getMessageType();
                        mainWorker.setState(new LeaderFollower(raftMessage.getSender()));
                        return;
                }
            }
            if (voteForMe>=clusterSize/2){
                mainWorker.setState(new Leader());
                return;
            }
            voteForMe = 0;
            Message message = new Message(null,jChannel.getAddress(),"2;"+selfID+";"+selfID+";"+ (++term));
            jChannel.send(message);
            Random random = new Random();
            int randomTimeout = (random.nextInt(4)+1)*1000;
            timeoutForCandidate = hashedWheelTimer.newTimeout(timerTask,randomTimeout, TimeUnit.MILLISECONDS);
        }
    }

    public void joinGroup(){
        candidateAddress = jChannel.getAddressAsString();
        candidateTerm = term;

        Message message = new Message(null,jChannel.getAddress(),"1;"+jChannel.getAddressAsString()+";Join in Group");
        try {
            jChannel.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fireWhenViewAccepted(View new_view, MainWorker mainWorker) {
        System.out.println("new viewAccepted");
        if (curState.get() == CANDIDATE && new_view.size()>= clusterSize/2){
            timeoutForCandidate = hashedWheelTimer.newTimeout(timerTask,0, TimeUnit.MILLISECONDS);
        }
    }

    public void fireWhenRaftMessageReceived(RaftMessage raftMessage) {

    }

}
