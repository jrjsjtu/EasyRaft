package EasyRaft;

/**
 * Created by jrj on 17-12-12.
 */
public class VotePara {
    long term;
    long lastLogIndex;
    long lastLogTerm;
    String candidateId;

    String result;

    VotePara(long term,String candidateId,long lastLogIndex,long lastLogTerm){
        this.term = term;this.candidateId = candidateId;this.lastLogIndex = lastLogIndex;this.lastLogTerm = lastLogTerm;
    }
    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public long getLastLogIndex() {
        return lastLogIndex;
    }

    public void setLastLogIndex(long lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public long getLastLogTerm() {
        return lastLogTerm;
    }

    public void setLastLogTerm(long lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
