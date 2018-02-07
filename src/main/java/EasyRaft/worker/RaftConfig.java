package EasyRaft.worker;

import KV.KVDatabase.ServerConfig;
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
public class RaftConfig extends BaseConfig{
    int clusterSize,port;
    RaftConfig(String[] filePath) throws Exception{
        if(filePath.length==1){
            String absolutePath = getAbsolutePath(filePath[0]);
            SAXReader reader = new SAXReader();
            Document document = reader.read(new File(absolutePath));
            Element root = document.getRootElement();
            initMap(root);
        }else{
            clusterSize = Integer.parseInt(filePath[0]);
            port =  Integer.parseInt(filePath[1]);
        }
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public int getPort() {
        return port;
    }

    private void initMap(Element node){
        Element elementIp = node.element("ip");
        Element elementSize = node.element("clusterSize");
        clusterSize = Integer.parseInt(elementSize.getText());
        port = Integer.parseInt(elementIp.getText().split(":")[1]);
    }


    public static void main(String[] args){
        try {
            System.out.println(System.getProperty("user.dir"));
            //RaftConfig clientConfig = new RaftConfig("/home/jrj/Desktop/idea-IU-163.12024.16/java_learn/EasyRaft/src/main/java/EasyRaft/worker/config.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
