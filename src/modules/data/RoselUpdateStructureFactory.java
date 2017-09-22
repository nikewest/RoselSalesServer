package modules.data;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class RoselUpdateStructureFactory extends UpdateStructureFactory{
    
    private static final Logger LOG = Logger.getLogger(RoselUpdateStructureFactory.class.getName());

    public RoselUpdateStructureFactory(UpdateItemFactory updateItemFactory) {
        super(updateItemFactory);
    }
        
    @Override
    public RoselUpdateStructure fillFromJSONString(String JSONString){
        
        RoselUpdateStructure updStr = null;
        
        try {            
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(JSONString);
            updStr = new RoselUpdateStructure((String) obj.get("table"));
            updStr.setUpdateVersion((long) obj.get("version"));
            JSONArray updates = (JSONArray) obj.get("updates");
            int len = updates.size();
            for(int i=0; i < len; i++){
                updStr.addUpdateItem(updateItemFactory.fillFromJSONString(((JSONObject) updates.get(i)).toJSONString()));
            }
        } catch (ParseException ex) {
            LOG.log(Level.SEVERE, null, ex);            
        }
        
        return updStr;
    }
    
}
