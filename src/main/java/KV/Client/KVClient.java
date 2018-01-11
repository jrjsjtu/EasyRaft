package KV.Client;

import EasyRaft.client.RaftClient;

import java.util.ArrayList;

/**
 * Created by jrj on 17-12-24.
 */
public class KVClient {
    private static int[] kvPorts = new int[]{10200,10201};
    private KVChannel kvChannel;
    KVClient(){
        try {
            kvChannel = new KVChannel();
            boolean succeed = false;
            for (int port : kvPorts){
                boolean tmp = kvChannel.connectServer("127.0.0.1",port);
                if (tmp){succeed = tmp;}
            }
            if (!succeed){
                throw new Exception("all server down");
            }
            kvChannel.getLeadrInfo();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args){
        /*
        KVClient kvClient = new KVClient();

        long start = System.currentTimeMillis();
        for(int i=0;i<300000;i++){
            kvClient.put("aaa"+i,"bbbb"+i);
        }
        System.out.println(System.currentTimeMillis() -start);
        kvClient.finish();
        */
        RaftClient raftClient = new RaftClient();
        try {
            raftClient.registerWatcher("test");
            ArrayList<String> strings =raftClient.getMemberList();


            if (strings.size()>=2){
                KVChannel kvChannel = new KVChannel();
                kvChannel.connectServer("127.0.0.1",10200);
                kvChannel.connectServer("127.0.0.1",10201);
                kvChannel.waitForConnection();
                long start = System.currentTimeMillis();
                for (long i=0;i<150000;i++){
                    kvChannel.put(1l,"aaa","bbb");
                    //kvChannel.put(1l,"aaa1","bbb");
                }
                System.out.println(System.currentTimeMillis() -start);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("end!!!!");
        /*
        try {
            KVChannel kvChannel = new KVChannel();
            long start = System.currentTimeMillis();
            for (long i=0;i<3;i++){
                kvChannel.put(i,"aaa","bbb");
                kvChannel.put(i,"aaa1","bbb");
            }
            System.out.println(System.currentTimeMillis() -start);
            //kvChannel.put(1,"aaa","bbb");
            //kvChannel.put(2,"aaa1","bbb1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }
}
