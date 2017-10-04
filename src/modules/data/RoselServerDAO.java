package modules.data;

import modules.serverlogic.DeviceInfo;
import java.util.ArrayList;
import java.util.Properties;

public interface RoselServerDAO {        
 
    public void setDataSourceSettings(Properties settings);
    
    public void initializeDataStructure();
    
    public String getClientName(long clientId);
    
    public ArrayList<String> getUpdates(long device_id, ArrayList<String> updateRequestInfo);        
    
    public DeviceInfo getDeviceInfo(String device_id);
    
    public void postOrdersFromJSON(long device_id, ArrayList<String> ordersInJSON);
}
