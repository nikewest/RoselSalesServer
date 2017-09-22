package modules.data;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class RoselUpdateItemFactory implements UpdateItemFactory{
        
    private static final Logger LOG = Logger.getLogger(RoselUpdateItemFactory.class.getName());

    @Override
    public RoselUpdateItem fillFromJSONString(String jsonString) {
        RoselUpdateItem dbItem = new RoselUpdateItem();
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);;
            dbItem.id = (long) jsonObject.get("id");
            dbItem.action = (int) ((long) jsonObject.get("action"));
            JSONArray jsonValues = (JSONArray) jsonObject.get("item_values");
            int len = jsonValues.size();
            for (int i = 0; i < len; i++) {
                JSONObject jsonValue = (JSONObject) jsonValues.get(i);
                String valueName = (String) jsonValue.get("name");
                String valueType = (String) jsonValue.get("type");
                String value = (String) jsonValue.get("value");
                dbItem.addItemValue(valueName, valueType, value);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception: ", ex);
            return null;
        }
        return dbItem;
    }
   
    public RoselUpdateItem fillFromResultSet(ResultSet set, int columnOffset) throws SQLException {        
        RoselUpdateItem dbItem = new RoselUpdateItem();
        dbItem.id = set.getLong("_id");        
        dbItem.action = set.getInt("action");
        ResultSetMetaData meta = set.getMetaData();
        int c = meta.getColumnCount();
        for (int i = 2 + columnOffset; i <= c; i++) {            
            dbItem.addItemValue(meta.getColumnName(i), meta.getColumnTypeName(i), set.getString(i));
        }
        return dbItem;
    }
    
}
