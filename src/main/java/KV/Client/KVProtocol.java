package KV.Client;

import java.io.InputStream;

/**
 * Created by jrj on 17-12-25.
 */
public interface KVProtocol {
    void put(String key,String value) throws Exception;
    String get(String key) throws Exception;
}
