package modules.serverlogic;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import modules.transport.ClientsRequestHandlerInterface;
import modules.transport.TransportMessage;
import roselsalesserver.db.DatabaseManager;

/**
 *
 * @author nikiforovnikita
 */
public class RequestHandler implements ClientsRequestHandlerInterface{
    
    //private final RoselServerModel server;    
    private final static Logger LOG = Logger.getLogger(RequestHandler.class.getName());        
    DatabaseManager databaseManager;
    
    public RequestHandler(RoselServerModel server) throws Exception {        
        databaseManager = DatabaseManager.getDatabaseManager(server.getServerSettings());        
    }

    @Override
    public void handleException(Exception ex) {
        LOG.log(Level.SEVERE, null, ex);
    }

    @Override
    public synchronized TransportMessage handleRequest(TransportMessage request) throws Exception {
        
        TransportMessage response = new TransportMessage();
        response.setDevice_id("SERVER");
        
        //check from who         
        //if check fails - send TYPE "NOT_REGISTERED"                        
        ClientModel clientModel= null;
        try {
            if(!checkDevice(request.getDevice_id(), clientModel)){
                response.setIntention(TransportMessage.NOT_REG);
                response.setEmptyBody();
                return response;
            }
        } catch (Exception ex) {            
            throw ex;
        }
        
        //check intention
        switch(request.getIntention()){
            case TransportMessage.GET: // TYPE "GET" - request for data updates 
                break;
            case TransportMessage.POST: // TYPE "POST" - request with orders
                break;
            default: //if wrong type?
                break;
        }
        
        return response;
    }
    
    public boolean checkDevice(String device_id, ClientModel clientModel) throws Exception{
        boolean checkResult = false;
        if (device_id == null) {
                return checkResult;
        }
        
        //get device_info
        String query = getDeviceInfoQuery(device_id);        
        
        try(ResultSet res = databaseManager.selectQuery(query)) {
            if (res.next()) {
                if (res.getBoolean("confirmed")) {                    
                    clientModel = new ClientModel();
                    clientModel.setId(res.getLong("_id"));
                    clientModel.setDevice_id(device_id);
                    clientModel.setManager_id(res.getLong("manager_id"));
                    clientModel.setName(query);
                    checkResult = true;
                } 
            } else {
                initializeDevice(device_id);
            }
        } catch (Exception ex) {            
            throw new Exception("Can't query to database");
        } 
        
        return checkResult;
    }
    
    void initializeDevice(String device_id) throws Exception {
       
        databaseManager.executeQuery(getNewDeviceQuery(device_id));
        
        ResultSet res = databaseManager.selectQuery(getDeviceInfoQuery(device_id));
        res.next();
        long savedID = res.getLong("_id");
        res.close();
        
        for (String tableName : getVersionTables()) {                        
            databaseManager.executeQuery(getNewVersionsTablesQuery(tableName, savedID));
        }
    }

    public static ArrayList<String> getVersionTables() {
        ArrayList<String> tables = new ArrayList();
        tables.add("PRODUCTS");
        tables.add("CLIENTS");
        tables.add("PRICES");
        tables.add("STOCK");
        tables.add("ADDRESSES");
        return tables;
    }

    private static String getDeviceInfoQuery(String device_id) {
        return "SELECT _id, device_id, name, manager_id, CASE ISNULL(manager_id,0) WHEN 0 THEN 0 ELSE 1 END AS confirmed FROM DEVICES WHERE device_id = '" + device_id + "'";        
    }
    
    private static String getNewDeviceQuery(String device_id){
        return "IF NOT EXISTS (SELECT device_id from DEVICES WHERE device_id = '" + device_id + "') "
                    + "INSERT INTO DEVICES (device_id) VALUES ('" + device_id + "');";
    }
    
    private static String getNewVersionsTablesQuery(String tableName, long _idOfDevice){
        return "IF NOT EXISTS (SELECT device_id from VERSIONS WHERE device_id = " + String.format("%d", _idOfDevice) + " AND table_name = '" + tableName + "')"
                    + "INSERT INTO VERSIONS (device_id, table_name, version) VALUES (" + String.format("%d", _idOfDevice) + ", '" + tableName + "', 0)"
                    + "ELSE "
                    + "UPDATE VERSIONS SET version = 0 WHERE device_id = " + String.format("%d", _idOfDevice) + " AND table_name = '" + tableName + "';";
    }
}
