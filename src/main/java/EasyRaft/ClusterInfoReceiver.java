package EasyRaft;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import java.nio.charset.Charset;

/**
 * Created by jrj on 17-12-20.
 */
public class ClusterInfoReceiver extends ReceiverAdapter {
    @Override
    public void receive(Message msg) {
        String line = msg.getSrc().toString();
        System.out.println(line);
    }

    @Override
    public void viewAccepted(View view) {
        //System.out.println("A client has changed！" + view.toString());
    }
}
