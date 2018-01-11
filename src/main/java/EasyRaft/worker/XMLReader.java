package EasyRaft.worker;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by jrj on 18-1-11.
 */
public class XMLReader {
    private int leaderPort;
    XMLReader(String path){
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(path);
            NodeList hostList = document.getElementsByTagName("port");

            //遍历每一个book节点
            for (int i = 0; i < hostList.getLength(); i++) {
                Node host = hostList.item(i);
                leaderPort = Integer.parseInt(host.getFirstChild().getNodeValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getLeaderPort(){
        return leaderPort;
    }
    public static void main(String[] args){
        new XMLReader("/home/jrj/Desktop/idea-IU-163.12024.16/java_learn/EasyRaft/src/main/java/EasyRaft/worker/config.xml");
    }
}

