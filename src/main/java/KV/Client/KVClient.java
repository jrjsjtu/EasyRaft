package KV.Client;

import EasyRaft.client.CtxProxy;
import EasyRaft.client.RaftClient;
import EasyRaft.client.callBack.RaftCallBack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;
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
        BufferedReader bf=new BufferedReader(new InputStreamReader(System.in));
        while(true){
            try {
                System.out.println("输入1代表写入KV,输入2代表查询KV");
                String s = bf.readLine();
                if(s.equals("1")){
                    System.out.println("请输入Key");
                    String key = bf.readLine();
                    System.out.println("请输入Value");
                    String value = bf.readLine();
                    kvClient.put(key,value);
                }else if(s.equals("2")){
                    System.out.println("请输入Key");
                    String key = bf.readLine();
                    System.out.println("key 为 " + key + "的 value 为");
                    System.out.println(kvClient.get(key));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
