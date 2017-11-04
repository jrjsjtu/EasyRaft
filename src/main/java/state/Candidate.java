package state;

import Utils.Timeout;
import Utils.TimerTask;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-10-30.
 */

public class Candidate extends State {
    Timeout timeoutForCandidate,timeoutPeriodic;
    int voteForMe;


    String leader,voter;
    int tmpState;

    static final int WaitForVote = 0;
    static final int CandidateWithLargerTerm = 1;
    static final int ReceiveLeaderHeartBeat = 2;

    public Candidate(){
        voteForMe = 0;tmpState = WaitForVote;
        Random random = new Random();
        timeoutForCandidate = hashedWheelTimer.newTimeout(new PeriodicTask(),0, TimeUnit.MILLISECONDS);
        timeoutPeriodic = hashedWheelTimer.newTimeout(new CandidateTimerTask(),1000, TimeUnit.MILLISECONDS);
        System.out.println("become candidate");
    }

    private class PeriodicTask implements TimerTask{
        public void run(Timeout timeout) throws Exception {
            voteForMe = 0;
            Message message = new Message(null,jChannel.getAddress(),RaftMessage.CandidateElection+";"+selfID+";"+selfID+";"+ (++term));
            jChannel.send(message);
            Random random = new Random();
            //这里随机超时
            int randomTimeout = (random.nextInt(5)+3)*1000;
            timeoutForCandidate = hashedWheelTimer.newTimeout(new PeriodicTask(),randomTimeout, TimeUnit.MILLISECONDS);
        }
    }

    private class CandidateTimerTask implements TimerTask{
        //思来想去,还是把处理信息做成定时任务 1是不用做线程同步  2.是状态转移比较方便
        public void run(Timeout timeout) throws Exception {
            //这里一次把消息处理完,决定转移到哪个状态
            while (!linkedBlockingQueue.isEmpty()){
                RaftMessage raftMessage;
                raftMessage = (RaftMessage) linkedBlockingQueue.take();
                System.out.println(raftMessage.getMessageType() +"  "+ raftMessage.getSender() +" "+ raftMessage.getTerm() + " " + term);
                switch (raftMessage.getMessageType()){
                    case RaftMessage.CandidateElection:
                        if(raftMessage.getTerm() > term && tmpState == WaitForVote){
                            tmpState = CandidateWithLargerTerm;
                            voter = raftMessage.getSender();
                            term = raftMessage.getTerm();
                        }else if(raftMessage.getTerm() == term && raftMessage.getMessageContent().equals(selfID) && tmpState == WaitForVote){
                            voteForMe++;
                        }
                        break;
                    case RaftMessage.LeaderHeartBeat:
                        if (tmpState != ReceiveLeaderHeartBeat){
                            tmpState = ReceiveLeaderHeartBeat;
                            term = raftMessage.getTerm();
                            leader = raftMessage.getSender();
                        }else if(raftMessage.getTerm()>term){
                            term = raftMessage.getTerm();
                            leader = raftMessage.getSender();
                        }
                        LeaderFollower leaderFollower = new LeaderFollower(raftMessage.getSender());
                        mainWorker.setState(leaderFollower);
                        timeoutForCandidate.cancel();
                        return;
                    default:
                        break;
                }
            }

            switch (tmpState){
                case CandidateWithLargerTerm:
                    Message message = new Message(null,jChannel.getAddress(),RaftMessage.CandidateElection+";"+selfID+";"+voter+";"+ (term));
                    System.out.println(RaftMessage.CandidateElection+";"+selfID+";"+voter+";"+ (term));
                    jChannel.send(message);
                    mainWorker.setState(new VoteFollower(voter));
                    timeoutPeriodic.cancel();
                    return;
                case ReceiveLeaderHeartBeat:
                    mainWorker.setState(new LeaderFollower(leader));
                    timeoutPeriodic.cancel();
                    return;
            }
            if (voteForMe>=clusterSize/2){
                mainWorker.setState(new Leader());
                timeoutForCandidate.cancel();
                return;
            }
            hashedWheelTimer.newTimeout(new CandidateTimerTask(),1000, TimeUnit.MILLISECONDS);
        }
    }

    public void joinGroup(){

    }

    public void fireWhenViewAccepted(View new_view, MainWorker mainWorker) {
        /*
        System.out.println("new viewAccepted");
        if (curState.get() == CANDIDATE && new_view.size()>= clusterSize/2){
            timeoutForCandidate = hashedWheelTimer.newTimeout(timerTask,0, TimeUnit.MILLISECONDS);
        }
        */
    }

    public void fireWhenRaftMessageReceived(RaftMessage raftMessage) {
        try {
            linkedBlockingQueue.put(raftMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
