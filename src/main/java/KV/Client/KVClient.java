package KV.Client;

import EasyRaft.client.CtxProxy;
import EasyRaft.client.RaftClient;
import EasyRaft.client.callBack.RaftCallBack;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import Utils.RaftLogger;
/**
 * Created by jrj on 17-12-24.
 */
public class KVClient {
    CtxProxy ctxProxy;
    KVChannel kvChannel;
    KVClient(ClientConfig clientConfig){
        try {
            ctxProxy = new CtxProxy();
            ArrayList<String> raftCluster = clientConfig.getIpPortList();
            for(String tmp:raftCluster){
                String[] info = tmp.split(":");
                try{
                    ctxProxy.connectWithoutHeartbeat(info[0],Integer.parseInt(info[1]));
                }catch (Exception e){
                    RaftLogger.log.info(e.getMessage());
                }
            }
            ArrayList<String> arrayList = ctxProxy.querySlot();

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

        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
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
        */
    }

    public void put(String key,String value) throws Exception{
        kvChannel.put(key,value);
    }

    public String get(String key) throws Exception{
        return kvChannel.get(key);
    }
    public static void main(String[] args){
        ClientConfig clientConfig = null;
        try {
            clientConfig = new ClientConfig(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        KVClient kvClient = new KVClient(clientConfig);
        long start = System.currentTimeMillis();
        try {
            kvClient.put("aaa1","ccc");
            System.out.println(kvClient.get("aaa1"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("finish uses " + (System.currentTimeMillis() -start));
    }
}
