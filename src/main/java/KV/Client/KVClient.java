package KV.Client;

import EasyRaft.client.RaftClient;
import EasyRaft.client.callBack.RaftCallBack;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by jrj on 17-12-24.
 */
public class KVClient {
    RaftClient raftClient;
    KVChannel kvChannel;
    AtomicInteger atomicInteger = new AtomicInteger(0);
    KVClient(){
        raftClient = new RaftClient();
        raftClient.joinRaft();
        ArrayList<String> arrayList = raftClient.getCurrentSlot();
        try {
            kvChannel = new KVChannel(2);
            for(String tmp:arrayList){
                String[] info = tmp.split(":");
                kvChannel.connectServer(info[0],Integer.parseInt(info[1]),Integer.parseInt(info[2]));
            }
            kvChannel.waitForConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void put(String key,String value) throws Exception{
        kvChannel.put(key,value);
    }

    public String get(String key) throws Exception{
        return kvChannel.get(key);
    }
    public static void main(String[] args){
        KVClient kvClient = new KVClient();
        long start = System.currentTimeMillis();

        try {
            kvClient.put("aaa1","ccc");
            System.out.println(kvClient.get("aaa1"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
        try {
            for (long i=0;i<150000;i++){
                kvClient.put("aaa","bbb");
                kvClient.put("aaa1","bbb");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        */
        System.out.println("finish uses " + (System.currentTimeMillis() -start));
    }
}
