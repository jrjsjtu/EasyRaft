package EasyRaft;

import EasyRaft.state.State;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-12-12.
 */
public abstract class RaftDelayedTask implements Runnable,Delayed {
    protected long time;
    protected State state;
    public boolean stillLastState(State tmpState){
        return state == tmpState;
    }
    public RaftDelayedTask(State state, long time){
        this.time = time;
        this.state = state;
    }
    public long getDelay(TimeUnit unit) {
        long r = unit.convert(time - System.currentTimeMillis(), TimeUnit.MICROSECONDS);
        return r;
    }

    public int compareTo(Delayed o) {
        if(this.time < ((RaftDelayedTask)o).time) return -1;
        else if(this.time > ((RaftDelayedTask)o).time)return 1;
        else return 0;
    }
}
