package EasyRaft.state;

/**
 * Created by jrj on 18-1-31.
 */
public interface CommitCallback {
    void executeAfterCommit(String log);
}
