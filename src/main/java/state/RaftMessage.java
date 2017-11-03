package state;

import org.jgroups.Message;

/**
 * Created by jrj on 17-11-2.
 */
public class RaftMessage {
    static final int LeaderHeartBeat = 0;
    static final int CandidateElection = 1;
    static final int LeaderLog = 2;

    int messageType;
    String sender,messageContent;
    long term;
    RaftMessage(Message message){
        String[] strList = ((String)message.getObject()).split(";");
        messageType = Integer.parseInt(strList[0]);
        sender = strList[1];
        messageContent = strList[2];
        term = Integer.parseInt(strList[3]);
    }

    long getTerm(){return term;}

    String getSender(){return sender;}

    int getMessageType(){return messageType;}

    String getMessageContent(){return messageContent;}
}
