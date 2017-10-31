package Utils;

import java.util.concurrent.TimeUnit;

/**
 * Created by jrj on 17-10-31.
 */
public class TestTimeWheel {
    public static void main(String[] args){
        HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();
        hashedWheelTimer.newTimeout(new TimerTask() {
            public void run(Timeout timeout) throws Exception {
                System.out.println("fuck");
            }
        },3000, TimeUnit.MILLISECONDS);
    }
}
