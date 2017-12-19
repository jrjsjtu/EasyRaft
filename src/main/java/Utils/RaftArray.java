package Utils;

import state.RaftLog;

import java.io.IOException;
import java.util.*;

/**
 * Created by jrj on 17-12-18.
 */
public class RaftArray{
    RaftLog head,tail;
    public RaftArray(){

    }

    public void add(RaftLog raftLog){
        if (head == null && tail == null){
            head = raftLog;tail = raftLog;
            return;
        }else if (raftLog.getIndex()<head.getIndex()){
            raftLog.setNext(head);
            head.setPrev(raftLog);
            head = raftLog;
            return;
        }else if (raftLog.getIndex()>tail.getIndex()){
            raftLog.setPrev(tail);
            tail.setNext(raftLog);
            tail = raftLog;
            return;
        }
        long headIndex= head.getIndex();
        long tailIndex= tail.getIndex();
        RaftLog curLog = head;
        RaftLog nextLog;
        while (curLog.getNext()!=null){
            nextLog = curLog.getNext();
            if (nextLog.getIndex()>raftLog.getIndex() && curLog.getIndex()<raftLog.getIndex()){
                raftLog.setNext(nextLog);raftLog.setPrev(curLog);
                nextLog.setPrev(raftLog);curLog.setNext(raftLog);
                System.out.println("warn!! there is a log with wrong index");
                return;
            }
            curLog = nextLog;
        }
        System.out.println("warn!! no suitable position found");
    }

    public void addToArrayList(ArrayList<RaftLog> arrayList){
        RaftLog tmp = head;
        while (tmp!=null){
            arrayList.add(tmp);
            tmp = tmp.getNext();
        }
    }

    public boolean checkIntegrity(){
        RaftLog tmp = head;
        if (tmp == null){
            return true;
        }
        while (tmp.getNext() != null){
            if (tmp.getNext().getIndex()-1!= tmp.getIndex()){
                return false;
            }
            tmp = tmp.getNext();
        }
        return true;
    }

    public static void main(String[] args){

    }
}
