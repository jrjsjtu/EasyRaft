package EasyRaft;

/**
 * Created by jrj on 17-12-12.
 */
public class AppendPara {
    long term;
    long prevLogIndex;
    long prevLogTerm;
    long leaderCommit;
    String leaderId;
    byte[] entries;

    String result;
    public AppendPara(long term,String leaderId,long prevLogIndex,long prevLogTerm,byte[] entries,long leaderCommit){
        this.term = term;this.leaderId = leaderId;this.prevLogIndex = prevLogIndex;this.prevLogTerm = prevLogTerm;
        this.entries = entries;this.leaderCommit = leaderCommit;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public long getPrevLogIndex() {
        return prevLogIndex;
    }

    public void setPrevLogIndex(long prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public long getPrevLogTerm() {
        return prevLogTerm;
    }

    public void setPrevLogTerm(long prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public long getLeaderCommit() {
        return leaderCommit;
    }

    public void setLeaderCommit(long leaderCommit) {
        this.leaderCommit = leaderCommit;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public byte[] getEntries() {
        return entries;
    }

    public void setEntries(byte[] entries) {
        this.entries = entries;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
