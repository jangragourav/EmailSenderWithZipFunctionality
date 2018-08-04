package test.Email;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MailConfig {

    Properties prop = new Properties();
    public void loadProp(){
        InputStream input = null;
        try {
            input = this.getClass().getResourceAsStream("/mail-template.properties");
            prop.load(input);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public String getValue(String key){
        return prop.getProperty(key);
    }
}
