package modules.data;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.simple.JSONObject;

public class RoselUpdateMap {
    
    private final ArrayList<String> updateBody;
    private final HashMap<String, Long> updateInfo;

    public RoselUpdateMap() {
        this.updateInfo = new HashMap<>();
        this.updateBody = new ArrayList<>();
    }
    
    public void putUpdateInfo(String table, Long version){
        getUpdateInfo().put(table, version);
    }
    
    public void addUpdate(String updateStr){
        getUpdateBody().add(updateStr);
    }

    public ArrayList<String> getUpdateBody() {
        return updateBody;
    }

    public HashMap<String, Long> getUpdateInfo() {
        return updateInfo;
    }
    
    public String getJsonUpdateInfo(){
        JSONObject jsonObj = new JSONObject(updateInfo);
        return jsonObj.toJSONString();
    }
    
}
