package EasyRaft.state;

/**
 * Created by jrj on 17-11-7.
 */
public interface RaftRpc {
    /**
     * The api for append entries into local log according to paper.
     *
     * @param term current term of the caller of the rpc
     * @param leaderId the string returned by JChannel.getAddress().toString();
     * @param prevLogIndex the index of the log previous to the last log contained in the entries.
     * @param prevLogTerm the term of the log previous to the last log contained in the entries.
     * @param entries the entries which will be appended to the local log if prevLogIndex and prevLogTerm match.
     *                The entries contains all the logs from prevLogIndex + 1;
     *                The entries can be null if there is no log during last heartbeat
     * @param leaderCommit the index of the log committed by the leader
     * @return the result of the rpc. The string is used for convenience.It contains the term,success or not and prevLogIndex of this rpc.
     *         The reason of the occurrence of prevLogIndex is that the rpc is asyn for caller, the response out of date must be filtered.
     */
    String AppendEntries(long term,String leaderId,long prevLogIndex,long prevLogTerm,byte[] entries,long leaderCommit);

    /**
     * The api for append entries into local log according to paper.
     *
     * @param term current term of the caller of the rpc
     * @param candidateId the string returned by JChannel.getAddress().toString();
     * @param lastLogIndex the index of the latest log of the callee.
     * @param lastLogTerm the term of the latest log of the callee.
     * @return the result of the rpc. The string is used for convenience.It contains the term,success or not of this rpc.
     */
    String RequestVote(long term,String candidateId,long lastLogIndex,long lastLogTerm);
}
