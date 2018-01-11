package EasyRaft.state;

/**
 * Created by jrj on 17-11-7.
 */
public interface RaftRpc {
    //这里其实有个问题,就是对于所有的RPC,都是有term>currentTerm要更新currentTerm的问题.现在要是两个rpc分别有不同的term就会有raceCondition,
    //但这仅仅用atomicLong是无法解决的!所以我的解决方法还是两个方法有一把公用的锁
    String AppendEntries(long term,String leaderId,long prevLogIndex,long prevLogTerm,byte[] entries,long leaderCommit);

    String RequestVote(long term,String candidateId,long lastLogIndex,long lastLogTer);
}
