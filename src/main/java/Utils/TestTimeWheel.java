package Utils;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-10-31.
 */
public class TestTimeWheel {
    public static void main(String[] args){
        HashMap AAA = new HashMap();
        AAA.put("aaa","aaa");
        HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();
        hashedWheelTimer.newTimeout(new TimerTask() {
            public void run(Timeout timeout) throws Exception {
                System.out.println("fuck");
            }
        },0, TimeUnit.MILLISECONDS);
    }
}
