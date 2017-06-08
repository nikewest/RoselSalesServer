package modules.serverlogic;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import modules.transport.AcceptClientException;
import modules.transport.ServerTransport;
import modules.transport.ServerTransportListener;
import modules.transport.StartServerException;
import modules.transport.TransportMessage;
import roselsalesserver.db.DatabaseManager;

/**
 *
 * @author nikiforovnikita
 */
public class RoselServerModel implements ServerTransportListener{
    
    private Properties serverSettings;    
    private ServerTransport transport;    
    private DatabaseManager dbManager;        
    private ArrayList<RoselServerModelObserverInterface> observers = new ArrayList<>(0);
    private static final Logger LOG = Logger.getLogger(RoselServerModel.class.getName());
    
    public void init() {  
        serverSettings = ServerSettings.getEmptySettings();        
        loadServerSettings();                        
    }

    public void applySettings(Properties settings){
        if(ServerSettings.checkSettings(settings)){
            serverSettings.putAll(settings);
            saveServerSettings();
        } else {
            notifyObservers("Can't save settings!");
        }
    }
    
    public void loadServerSettings() {        
        File settingsFile = new File("settings");
        if (settingsFile.exists()) {
            try (FileReader fileReader = new FileReader(settingsFile)) {
                serverSettings.load(fileReader);
                if (ServerSettings.checkSettings(serverSettings)) {
                    return;
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }        
        serverSettings = ServerSettings.getEmptySettings();
        saveServerSettings();
    }
    
    public void saveServerSettings(){
        File settingsFile = new File("settings");
        try {
            if (!settingsFile.exists()) {
                settingsFile.createNewFile();
            }
            try (FileWriter fileWriter = new FileWriter(settingsFile)) {
                serverSettings.store(fileWriter, null);
                fileWriter.flush();
                notifyObservers("Settings saved");
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception: ", ex);
            notifyObservers("Can't save settings!");
        }
    }
    
    public Properties getServerSettings() {
        return serverSettings;
    }
    
    public DatabaseManager getDbManager(){
        return dbManager;
    }
    
    // TRANSPORT
    public void startServer(){
        
        try {
            dbManager = DatabaseManager.getDatabaseManager(serverSettings);            
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            notifyObservers("Can't connect to DB!");
            return;
        }
        
        if(transport==null){
            transport = new ServerTransport();
            transport.setTransportListener(this);
        }        
        try {
            transport.start();            
            notifyStateChanged();            
        } catch (StartServerException | AcceptClientException ex) {
            LOG.log(Level.SEVERE, null, ex);
            notifyObservers("Can't start server!");
            return;
        }
    }
    
    public void stopServer(){
        transport.stop();
        dbManager.endWork();
        notifyStateChanged();
    }
    
    public void notifyStateChanged(){
        for(RoselServerModelObserverInterface obs : observers){
            obs.onServerStateChange(transport.isStarted());
        }
    }
    
    public void notifyObservers(String msg){
        for(RoselServerModelObserverInterface obs : observers){
            obs.handleMsg(msg);
        }
    }
    
    public void registerRoselServerModelObserver(RoselServerModelObserverInterface obs){
        observers.add(obs);
    }
    
    public void unregisterRoselServerModelObserver(RoselServerModelObserverInterface obs){
        observers.remove(obs);
    }

    @Override
    public void handleTransportException(Exception ex) {
        if(ex instanceof AcceptClientException){
            notifyStateChanged();
        }
    }

    @Override
    public TransportMessage handleClientRequest(TransportMessage request, ClientModel clientModel) throws Exception {
        TransportMessage response = new TransportMessage();
        response.setDevice_id("SERVER");
        
        if(clientModel==null){
            response.setIntention(TransportMessage.NOT_REG);
            response.setEmptyBody();
            return response;
        }
        
        //check intention
        switch(request.getIntention()){
            case TransportMessage.GET: // TYPE "GET" - request for data updates 
                response.setIntention(TransportMessage.UPDATE);
                response.setBody(getUpdates(clientModel));                
                break;
            case TransportMessage.POST: // TYPE "POST" - request with orders
                postOrders(request.getBody());
                response.setIntention(TransportMessage.POST_COMMIT);
                break;
            case TransportMessage.UPDATE_COMMIT:
                commitClientsUpdate(clientModel);                
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
        
        try(ResultSet res = dbManager.selectQuery(getDeviceInfoQuery(device_id))) {
            if (res.next()) {
                if (res.getBoolean("confirmed")) {                    
                    clientModel = new ClientModel();
                    clientModel.setId(res.getLong("_id"));
                    clientModel.setDevice_id(device_id);
                    clientModel.setManager_id(res.getLong("manager_id"));
                    clientModel.setName(res.getString("name"));
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
        dbManager.executeQuery(getNewDeviceQuery(device_id));        
        ResultSet res = dbManager.selectQuery(getDeviceInfoQuery(device_id));
        res.next();
        long savedID = res.getLong("_id");
        res.close();        
        for (String tableName : getVersionTables()) {                        
            dbManager.executeQuery(getNewVersionsTablesQuery(tableName, savedID));
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
    
    private ArrayList<String> getUpdates(ClientModel clientModel) throws Exception{        
        ArrayList<String> updates = new ArrayList<String>(0);
        ServerDbItemFactory factory = new ServerDbItemFactory();
        clientModel.setUpdatedTableVersions(getTableVersionsMap());
        HashMap<String,Long> updatedTableVersions = clientModel.getUpdatedTableVersions();
        ResultSet resSet;
        int updateSize = 0;
        
        //CLIENTS UPDATES        
        resSet = dbManager.selectQuery(getClientsUpdatesQuery(clientModel));                
        updateSize = resSet.getFetchSize();
        while (resSet.next()) {
            updates.add(factory.fillFromResultSet(resSet, "CLIENTS", 2).toString());
            long lastVersion = (long) updatedTableVersions.get("CLIENTS");
            if (lastVersion < resSet.getLong("version")) {
                lastVersion = resSet.getLong("version");                
            }            
            updatedTableVersions.put("CLIENTS", lastVersion);
        }
        resSet.close();
        
        //PRODUCTS UPDATES
        resSet = dbManager.selectQuery(getProductsUpdatesQuery(clientModel));                
        updateSize = resSet.getFetchSize();
        while (resSet.next()) {
            updates.add(factory.fillFromResultSet(resSet, "PRODUCTS", 2).toString());
            long lastVersion = (long) updatedTableVersions.get("PRODUCTS");
            if (lastVersion < resSet.getLong("version")) {
                lastVersion = resSet.getLong("version");                
            }            
            updatedTableVersions.put("PRODUCTS", lastVersion);
        }
        resSet.close();
        
        //ADDRESSES UPDATES        
        resSet = dbManager.selectQuery(getAddressesUpdatesQuery(clientModel));                
        updateSize = resSet.getFetchSize();
        while (resSet.next()) {
            updates.add(factory.fillFromResultSet(resSet, "ADDRESSES", 2).toString());
            long lastVersion = (long) updatedTableVersions.get("ADDRESSES");
            if (lastVersion < resSet.getLong("version")) {
                lastVersion = resSet.getLong("version");                
            }            
            updatedTableVersions.put("ADDRESSES", lastVersion);
        }
        resSet.close();
        
        //PRICES UPDATES
        resSet = dbManager.selectQuery(getPricesUpdatesQuery(clientModel));                
        updateSize = resSet.getFetchSize();
        while (resSet.next()) {
            updates.add(factory.fillFromResultSet(resSet, "PRICES", 2).toString());
            long lastVersion = (long) updatedTableVersions.get("PRICES");
            if (lastVersion < resSet.getLong("version")) {
                lastVersion = resSet.getLong("version");                
            }            
            updatedTableVersions.put("PRICES", lastVersion);
        }
        resSet.close();
        
        //STOCK UPDATES         
        resSet = dbManager.selectQuery(getStockUpdatesQuery(clientModel));                
        updateSize = resSet.getFetchSize();
        while (resSet.next()) {
            updates.add(factory.fillFromResultSet(resSet, "STOCK", 2).toString());
            long lastVersion = (long) updatedTableVersions.get("STOCK");
            if (lastVersion < resSet.getLong("version")) {
                lastVersion = resSet.getLong("version");                
            }            
            updatedTableVersions.put("STOCK", lastVersion);
        }
        resSet.close();
        
        return updates;
    }
    
    private static HashMap<String, Long> getTableVersionsMap(){
        HashMap updatedTableVersions = new HashMap<String, Long>(getVersionTables().size());
        for (String tableName : getVersionTables()) {
            updatedTableVersions.put(tableName, 0);
        }
        return updatedTableVersions;
    }
    
    private String getClientsUpdatesQuery(ClientModel clientModel){
        return "SELECT temp.version, temp.action, C.* FROM "
                + "(SELECT "
                + "	U.item_id,"
                + "	MAX(U._id) AS version,"
                + "	MIN(U.action) AS action,"
                + "	V.device_id "
                + "FROM VERSIONS AS V"
                + "	INNER JOIN UPDATES AS U ON (V.version < U._id) AND V.table_name = 'CLIENTS' AND V.device_id = " + clientModel.getId() + " AND U.table_name = 'CLIENTS' "
                + "GROUP BY U.item_id, V.device_id) AS temp "
                + "     INNER JOIN CLIENTS AS C ON C._id = temp.item_id AND C.manager_id = " + clientModel.getManager_id();                
    }
    
    private String getProductsUpdatesQuery(ClientModel clientModel){
        return "SELECT temp.version, temp.action, P.* FROM "
                + "(SELECT "
                + "	U.item_id,"
                + "	MAX(U._id) AS version,"
                + "	MIN(U.action) AS action,"
                + "	V.device_id "
                + "FROM VERSIONS AS V"
                + "	INNER JOIN UPDATES AS U ON (V.version < U._id) AND V.table_name = 'PRODUCTS' AND V.device_id = " + clientModel.getId() + " AND U.table_name = 'PRODUCTS' "
                + "GROUP BY U.item_id, V.device_id) AS temp "
                + "     INNER JOIN PRODUCTS AS P ON P._id = temp.item_id";                
    }
    
    private String getAddressesUpdatesQuery(ClientModel clientModel){
        return "SELECT temp.version, temp.action, A.* FROM "
                + "(SELECT "
                + "	U.item_id,"
                + "	MAX(U._id) AS version,"
                + "	MIN(U.action) AS action,"
                + "	V.device_id "
                + "FROM VERSIONS AS V"
                + "	INNER JOIN UPDATES AS U ON (V.version < U._id) AND V.table_name = 'ADDRESSES' AND V.device_id = " + clientModel.getId() + " AND U.table_name = 'ADDRESSES' "
                + "GROUP BY U.item_id, V.device_id) AS temp "
                + "     INNER JOIN ADDRESSES AS A ON A._id = temp.item_id "
                + "     INNER JOIN CLIENTS AS C ON A.client_id = C._id AND C.manager_id = " + clientModel.getManager_id();                
    }
    
    private String getPricesUpdatesQuery(ClientModel clientModel) {
        return "SELECT temp.version, temp.action, P.* FROM "
                + "(SELECT "
                + "	U.item_id,"
                + "	MAX(U._id) AS version,"
                + "	MIN(U.action) AS action,"
                + "	V.device_id "
                + "FROM VERSIONS AS V"
                + "	INNER JOIN UPDATES AS U ON (V.version < U._id) AND V.table_name = 'PRICES' AND V.device_id = " + clientModel.getId() + " AND U.table_name = 'PRICES' "
                + "GROUP BY U.item_id, V.device_id) AS temp "
                + "       INNER JOIN PRICES AS P ON P._id = temp.item_id "
                + "       INNER JOIN CLIENTS AS C ON P.client_id = C._id AND C.manager_id = " + clientModel.getManager_id();
    }
    
    private String getStockUpdatesQuery(ClientModel clientModel){
        return "SELECT temp.version, temp.action, S.* FROM "
                + "(SELECT "
                + "	U.item_id,"
                + "	MAX(U._id) AS version,"
                + "	MIN(U.action) AS action,"
                + "	V.device_id "
                + "FROM VERSIONS AS V"
                + "	INNER JOIN UPDATES AS U ON (V.version < U._id) AND V.table_name = 'STOCK' AND V.device_id = " + clientModel.getId() + " AND U.table_name = 'STOCK' "
                + "GROUP BY U.item_id, V.device_id) AS temp "
                + "     INNER JOIN STOCK AS S ON S._id = temp.item_id";                
    }

    private boolean postOrders(ArrayList<String> orders){
        return true;
    }        
    
    @Override
    public void handleClientHandlerException(Exception ex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ClientModel buildClientModel(TransportMessage request) {
        //check from who? if check fails - send TYPE "NOT_REGISTERED"                        
        ClientModel clientModel = null;
        try {
            if(!checkDevice(request.getDevice_id(), clientModel)){                
                return null;
            }
        } catch (Exception ex) {             
            return null;
        }
        return clientModel;
    }

    @Override
    public void commitClientsUpdate(ClientModel clientModel) {
        HashMap updatedTableVersions = clientModel.getUpdatedTableVersions();
        for (Iterator it = updatedTableVersions.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            if ((Long) entry.getValue() > 0) {
                String updateClientTableVersionsQuery = "UPDATE VERSIONS SET version = " + String.format("%d", entry.getValue()) + " WHERE table_name = '" + (String) entry.getKey() + "' and device_id = " + clientModel.getId();
                try {                
                    dbManager.executeQuery(updateClientTableVersionsQuery);
                } catch (Exception ex) {
                    Logger.getLogger(RoselServerModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }        
    }    
    
}
