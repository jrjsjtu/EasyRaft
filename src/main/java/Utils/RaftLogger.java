package Utils;

import org.apache.log4j.Logger;

/**
 * Created by jrj on 18-2-7.
 */
public class RaftLogger {
    public static Logger log = Logger.getLogger(RaftLogger.class.getClass());
    public static void main(String[] args){
        log.info("testtt");
    }
}
