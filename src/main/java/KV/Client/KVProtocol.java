package KV.Client;

import java.io.InputStream;

/**
 * Created by jrj on 17-12-25.
 */
public interface KVProtocol {
    void put(long requestIndex,String key,String value) throws Exception;
}
