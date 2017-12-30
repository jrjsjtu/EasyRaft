package KV.Client;

/**
 * Created by jrj on 17-12-24.
 */
public class KVClient {
    private int[] ports = new int[]{30303,30304,30305};
    private KVChannel kvChannel;
    KVClient(){
        try {
            kvChannel = new KVChannel();
            ports = new int[]{30303,30304,30305};
            boolean succeed = false;
            for (int port : ports){
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
        try {
            KVChannel kvChannel = new KVChannel();
            long start = System.currentTimeMillis();
            for (long i=0;i<3;i++){
                kvChannel.put(i,"aaa","bbb");
            }
            System.out.println(System.currentTimeMillis() -start);
            //kvChannel.put(1,"aaa","bbb");
            //kvChannel.put(2,"aaa1","bbb1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
