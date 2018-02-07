package Utils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by jrj on 18-2-7.
 */
public class BaseConfig {
    protected String getAbsolutePath(String path) throws FileNotFoundException{
        if(path.charAt(0)=='/' && new File(path).exists()){
            return path;
        }else{
            String curPath = System.getProperty("user.dir");
            String tmp = curPath+"/"+path;
            if(new File(tmp).exists()){
                return tmp;
            }
        }
        throw new FileNotFoundException();
    }
}
