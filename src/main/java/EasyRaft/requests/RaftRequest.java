package EasyRaft.requests;

/**
 * Created by jrj on 18-1-27.
 */
public interface RaftRequest {
    void processResult(String result);
    void waitForResponse();
}
