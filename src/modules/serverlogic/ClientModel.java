package modules.serverlogic;

import java.util.HashMap;

public class ClientModel {
    private long manager_id;   
    private String name;
    private String device_id;
    private long _id;
    private HashMap<String,Long> updatedTableVersions;
    
    public long getManager_id() {
        return manager_id;
    }

    public void setManager_id(long manager_id) {
        this.manager_id = manager_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public long getId() {
        return _id;
    }

    public void setId(long _id) {
        this._id = _id;
    }

    public HashMap<String,Long> getUpdatedTableVersions() {
        return updatedTableVersions;
    }

    public void setUpdatedTableVersions(HashMap<String,Long> updatedTableVersions) {
        this.updatedTableVersions = updatedTableVersions;
    }
}
