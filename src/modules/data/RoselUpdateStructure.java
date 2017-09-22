package modules.data;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class RoselUpdateStructure {
    private ArrayList<RoselUpdateItem> updateItems = new ArrayList<RoselUpdateItem>();
    private String tableName;
    private Long updateVersion = (long) 0;

    public RoselUpdateStructure(String tableName) {        
        this.tableName = tableName;
    }
    
    public ArrayList<RoselUpdateItem> getUpdateItems() {
        return updateItems;
    }

    public void setUpdateItems(ArrayList<RoselUpdateItem> updateItems) {
        this.updateItems = updateItems;
    }

    public Long getUpdateVersion() {
        return updateVersion;
    }

    public void setUpdateVersion(Long updateVersion) {
        this.updateVersion = updateVersion;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public boolean isEmpty(){
        return updateItems!=null && updateItems.isEmpty();
    }
    
    public void addUpdateItem(RoselUpdateItem item){
        updateItems.add(item);
    }
    
    public JSONObject toJSONObject(){
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("table", getTableName());
        jsonObj.put("version", getUpdateVersion());
        JSONArray items = new JSONArray();
        //if(getUpdateItems()!=null) {
            for (RoselUpdateItem curItem : getUpdateItems()) {
                items.add(curItem.toJSONObject());
            }
            jsonObj.put("updates", items);
        //}
        return jsonObj;
    }
}
