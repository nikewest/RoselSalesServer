package roselsalesserver;

import java.util.HashMap;

public class MobileClient {
    
    private long _id;    
    private String name;
    private String device_id;
    private boolean confirmed;
    private Manager manager;
    
    private HashMap<String, Long> updatedTableVersions = new HashMap<>();
    
    public MobileClient(long _id, String device_id) {
        this._id = _id;
        this.device_id = device_id;
    }

    public String getDevice_id() {
        return device_id;
    }

    public long getId() {
        return _id;
    }    

    public HashMap getUpdatedTableVersions() {
        return updatedTableVersions;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setId(long _id) {
        this._id = _id;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }    

    /**
     * @return the manager
     */
    public Manager getManager() {
        return manager;
    }

    /**
     * @param manager the manager to set
     */
    public void setManager(Manager manager) {
        this.manager = manager;
    }
}
