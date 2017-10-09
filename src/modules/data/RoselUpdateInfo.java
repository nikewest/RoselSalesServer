package modules.data;

import java.util.ArrayList;
import org.json.simple.JSONObject;

public class RoselUpdateInfo {
    
    private String table;
    private long version;
    private long amount;    
    private ArrayList<RoselUpdateItem> updateItems = new ArrayList<RoselUpdateItem>();

    public RoselUpdateInfo(String table) {
        this.table = table;
    }
    
    public RoselUpdateInfo(String table, long version, long amount) {
        this.table = table;
        this.version = version;
        this.amount = amount;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
    
    public void addUpdateItem(RoselUpdateItem updateItem){
        getUpdateItems().add(updateItem);
    }
    
    public boolean isEmpty(){
        return getUpdateItems()!=null && getUpdateItems().isEmpty();
    }
    
    public JSONObject toJSON(){
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("table", getTable());
        jsonObj.put("version", getVersion());
        jsonObj.put("amount", getAmount());
        return jsonObj; 
    }    

    public ArrayList<RoselUpdateItem> getUpdateItems() {
        return updateItems;
    }
    
}
