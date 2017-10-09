package modules.data;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public abstract class RoselJsonParser {
    
    private static final Logger LOG = Logger.getLogger(RoselJsonParser.class.getName());
    
    public static RoselUpdateInfo fromJSONString(String jsonString){        
        RoselUpdateInfo updateInfo = null;
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(jsonString);            
            
            updateInfo = new RoselUpdateInfo((String) jsonObj.get("table"),(long) jsonObj.get("version"),(long) jsonObj.get("amount")); 
            
//            String tableName = (String) jsonObj.get("table");
//            Long version = (long) jsonObj.get("version");
//            Integer amount = (Integer) jsonObj.get("amount");           
//            updateInfo = new RoselUpdateInfo(tableName,version, amount);            
            
        } catch (Exception ex) {            
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return updateInfo;
    }
    
}
