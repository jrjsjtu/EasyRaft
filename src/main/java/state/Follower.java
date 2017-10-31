package state;

import Utils.Timeout;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import worker.MainWorker;

import Utils.Timeout;
import Utils.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-10-30.
 */
public class Follower extends State {
    Timeout timeoutForCandidate;
    TimerTask timerTask;
    public Follower(){
        curState = FOLLOWER;
        timerTask = new TimerTask() {
            public void run(Timeout timeout) throws Exception {
                Message message = new Message(null,jChannel.getAddress(),"2:"+jChannel.getAddressAsString()+";Becoming:candidate;term:"+ term++);
                jChannel.send(message);
                System.out.println("I am candidate");
                //这里有问题,如果执行到这里外边执行timeoutForCandidate.cancel()就会引发错误
                timeoutForCandidate = hashedWheelTimer.newTimeout(timerTask,3000, TimeUnit.MILLISECONDS);
            }
        };
    }

    public void joinGroup(){
        Message message = new Message(null,jChannel.getAddress(),"1:"+jChannel.getAddressAsString()+";Join in Group");
        timeoutForCandidate = hashedWheelTimer.newTimeout(timerTask,3000, TimeUnit.MILLISECONDS);
        try {
            jChannel.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fireWhenViewAccepted(View new_view, MainWorker mainWorker) {
        System.out.println("currentSize " + new_view.size());
    }

    public void fireWhenMessageReceived(Message msg, MainWorker mainWorker) {

    }
}
