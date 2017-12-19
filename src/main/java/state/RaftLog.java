package state;

/**
 * Created by jrj on 17-11-7.
 */
public class RaftLog {
    long term;long index;
    String log;
    RaftLog prev,next;

    public RaftLog(long term, long index, byte[] log){
        this.term = term;this.index = index;this.log = new String(log);
    }

    public RaftLog(long term,long index,String log){
        this.term = term;this.index = index;this.log = log;
    }


    public long getTerm(){return term;}
    public long getIndex(){return index;}
    public byte[] getLog(){
        return log.getBytes();
    }

    public RaftLog getPrev() {
        return prev;
    }

    public void setPrev(RaftLog prev) {
        this.prev = prev;
    }

    public RaftLog getNext() {
        return next;
    }

    public void setNext(RaftLog next) {
        this.next = next;
    }

    public boolean moreUpToDate(long lastLogIndex,long lastLogTerm){
        if (lastLogTerm == term){
            return index>=lastLogIndex;
        }else{
            return term>lastLogIndex;
        }
    }
}
