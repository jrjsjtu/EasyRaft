package state;

/**
 * Created by jrj on 17-11-7.
 */
public class RaftLog {
    long term;long index;
    byte[] log;
    RaftLog(long term,long index,byte[] log){
        this.term = term;this.index = index;this.log = log;
    }
    long getTerm(){return term;}
    long getIndex(){return index;}
}
