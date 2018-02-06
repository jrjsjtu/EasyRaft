package KV.Client;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by jrj on 18-2-6.
 */
public class ClientConfig {
    ArrayList<String> ipPortList = new ArrayList<String>();
    ClientConfig(String filePath) throws Exception{
        SAXReader reader = new SAXReader();
        Document document = reader.read(new File(filePath));
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
    }

    public ArrayList<String> getIpPortList(){
        return ipPortList;
    }

    public static void main(String[] args){
        try {
            ClientConfig clientConfig = new ClientConfig("/home/jrj/Desktop/idea-IU-163.12024.16/java_learn/EasyRaft/src/main/java/KV/Client/config.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
