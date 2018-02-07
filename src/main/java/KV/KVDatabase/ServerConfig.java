package KV.KVDatabase;

import Utils.BaseConfig;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by jrj on 18-2-6.
 */
public class ServerConfig extends BaseConfig {
    ArrayList<String> ipPortList = new ArrayList<String>();
    int serverPort,shard;
    ServerConfig(String filePath) throws Exception{
        String absolutePath = getAbsolutePath(filePath);
        SAXReader reader = new SAXReader();
        Document document = reader.read(new File(absolutePath));
        Element root = document.getRootElement();
        initMap(root);
    }

    private void initMap(Element node){
        Element element = node.element("host");
        Iterator<Element> iterator = element.elementIterator();
        while(iterator.hasNext()){
            Element elementInside = iterator.next();
            ipPortList.add(elementInside.getText());
        }
        element = node.element("KVServerPort");
        serverPort = Integer.parseInt(element.getText());

        element = node.element("KVSharding");
        shard = Integer.parseInt(element.getText());
    }

    public ArrayList<String> getIpPortList(){
        return ipPortList;
    }
    public int getserverPort(){
        return serverPort;
    }
    public int getshard(){
        return shard;
    }

    public static void main(String[] args){
        try {
            ServerConfig clientConfig = new ServerConfig("//home/jrj/Desktop/idea-IU-163.12024.16/java_learn/EasyRaft/src/main/java/KV/KVDatabase/config.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
