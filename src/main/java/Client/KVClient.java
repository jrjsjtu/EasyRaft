package Client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Created by jrj on 17-12-24.
 */
public class KVClient {
    Socket socket;
    OutputStream outputStream;
    InputStream inputStream;
    private static long index;
    private static String prefix;
    private static HashMap hashMap;

    static{
        hashMap = new HashMap<String,OutputStream>();
    }

    KVClient(){
        try {
            socket=new Socket("localhost",30303);
            socket.setTcpNoDelay(true);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] bytes){
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(int size){
        try {
            outputStream.write(size);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void finish(){
        try {
            if(inputStream.read()>0){

            }
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put(String key,String value){
        int payLoadSize = key.length() + value.length() + 8;
        ByteBuffer byteBuffer  = ByteBuffer.allocate(payLoadSize+4);
        byteBuffer.put(intToByteArray(payLoadSize));
        byteBuffer.put(intToByteArray(key.length()));
        byteBuffer.put(key.getBytes()).put(intToByteArray(value.length())).put(value.getBytes());
        try {
            outputStream.write(byteBuffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
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
            for (long i=0;i<300000;i++){
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
