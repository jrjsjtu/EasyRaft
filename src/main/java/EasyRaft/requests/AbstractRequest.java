package EasyRaft.requests;

import java.util.concurrent.Semaphore;

/**
 * Created by jrj on 18-1-28.
 */
public abstract class AbstractRequest implements RaftRequest{
    Thread curThread;
    Semaphore semaphore;
    AbstractRequest(){
        curThread = Thread.currentThread();
        semaphore = new Semaphore(0);
    }

    public void waitForResponse(){
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void notifyResponse(){
        semaphore.release();
    }
}
