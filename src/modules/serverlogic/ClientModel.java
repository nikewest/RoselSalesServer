package modules.serverlogic;

/**
 *
 * @author nikiforovnikita
 */
public class ClientModel {
    private long manager_id;   
    private String name;
    private String device_id;
    private long _id;

    /**
     * @return the manager_id
     */
    public long getManager_id() {
        return manager_id;
    }

    /**
     * @param manager_id the manager_id to set
     */
    public void setManager_id(long manager_id) {
        this.manager_id = manager_id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the device_id
     */
    public String getDevice_id() {
        return device_id;
    }

    /**
     * @param device_id the device_id to set
     */
    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    /**
     * @return the _id
     */
    public long getId() {
        return _id;
    }

    /**
     * @param _id the _id to set
     */
    public void setId(long _id) {
        this._id = _id;
    }
}
