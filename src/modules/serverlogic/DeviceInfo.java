package modules.serverlogic;

public class DeviceInfo {
 
    private String device_id;
    private String name;
    private boolean confirmed;
    private long innerId;

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public long getInnerId() {
        return innerId;
    }

    public void setInnerId(long innerId) {
        this.innerId = innerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
}
