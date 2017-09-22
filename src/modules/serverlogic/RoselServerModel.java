package modules.serverlogic;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import modules.data.RoselServerDAO;
import modules.transport.AcceptClientException;
import modules.transport.ServerTransport;
import modules.transport.TransportException;
import modules.transport.TransportMessage;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class RoselServerModel {
    
    private Properties serverSettings;    
    private ServerTransport transport;    
    ConfigurableApplicationContext context;
    private RoselServerDAO roselServerDAO;        
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
        File settingsFile = new File("settings.properties");
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
    
//    public DatabaseManager getDbManager(){
//        return dbManager;
//    }
    
    public RoselServerDAO getDAO(){
        return roselServerDAO;
    }
        
    // TRANSPORT
    public void startServer(){
        
        try {
//            dbManager = DatabaseManager.getDatabaseManager(serverSettings); 
//            dbManager.initConnection();
            context = new AnnotationConfigApplicationContext(RoselSpringConf.class);
            roselServerDAO = context.getBean(RoselServerDAO.class);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            notifyObservers("Can't connect to DB!");
            return;
        }
        
        if(transport==null){
            transport = new ServerTransport();
            transport.setRoselServer(this);
        }        
        try {
            transport.start();            
            notifyStateChanged();            
        } catch (TransportException | AcceptClientException ex) {
            LOG.log(Level.SEVERE, null, ex);
            notifyObservers("Can't start server!");
            stopServer();            
        }
    }
    
    public void stopServer(){        
//        if(dbManager!=null){
//            dbManager.endWork();
//        }
        if(context!=null){
            context.registerShutdownHook();
        }
        
        if(transport!=null && transport.isStarted()){
            transport.stop();
            notifyStateChanged();
        }        
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
    
    public synchronized void handleTransportException(Exception ex) {        
        LOG.log(Level.SEVERE, "Transport Exception:", ex);
        stopServer();
    }

    public synchronized TransportMessage handleClientRequest(TransportMessage request, DeviceInfo deviceInfo) throws SQLException, ParseException {
        TransportMessage response = new TransportMessage();
        response.setDevice_id(TransportMessage.SERVER_ID);        
        if(!deviceInfo.isConfirmed()){
            response.setIntention(TransportMessage.NOT_REG);
            response.setEmptyBody();
            return response;
        }
        
        //check intention
        switch(request.getIntention()){
            case TransportMessage.GET: // TYPE "GET" - request for data updates 
                response.setIntention(TransportMessage.UPDATE);
                response.setBody(getUpdates(deviceInfo, request.getBody()));                
                break;
            case TransportMessage.POST: // TYPE "POST" - request with orders
                postOrders(request.getBody(), deviceInfo);
                response.setIntention(TransportMessage.POST_COMMIT);
                break;                
            default: //if wrong type?
                break;
        }
        
        return response;
    }
    
    public static ArrayList<String> getVersionTables() {
        ArrayList<String> tables = new ArrayList();
        tables.add("PRODUCTS");
        tables.add("CLIENTS");
        tables.add("PRICES");        
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
    
    private ArrayList<String> getUpdates(DeviceInfo deviceInfo, ArrayList<String> requestBody) {
        
        return roselServerDAO.getUpdates(deviceInfo.getInnerId(), requestBody);
        
    }
    
    private static HashMap<String, Long> getTableVersionsMap(){
        HashMap<String, Long> updatedTableVersions = new HashMap<String, Long>(getVersionTables().size());
        for (String tableName : getVersionTables()) {
            updatedTableVersions.put(tableName, new Long(0));
        }
        return updatedTableVersions;
    }
    
    private void postOrders(ArrayList<String> ordersInJSON, DeviceInfo deviceInfo) throws SQLException, ParseException {
        postOrdersInJSON(ordersInJSON, deviceInfo);
        if (ordersInJSON.size() > 0) {            
            try {            
                sendNotificationsForOrders(ordersInJSON, deviceInfo);
            } catch (MessagingException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }        
    }  
    
    public void postOrdersInJSON(ArrayList<String> ordersInJSON, DeviceInfo deviceInfo) throws SQLException, ParseException {  
        
        roselServerDAO.postOrdersFromJSON(deviceInfo.getInnerId(),ordersInJSON);
        
    }
    
    public void sendNotificationsForOrders(ArrayList<String> ordersInJSONArrayList, DeviceInfo deviceInfo) throws ParseException, SQLException, MessagingException {        
        for (String jsonString : ordersInJSONArrayList) {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
            StringBuilder msgText = new StringBuilder();
            msgText.append("Поступил новый заказ из мобильного приложения");
            if (deviceInfo.getName() != null && deviceInfo.getName().length() > 0) {
                msgText.append(" от ");
                msgText.append(deviceInfo.getName());
            }
            msgText.append('\n').append("Клиент: ").append(getClientNameByID((Long) jsonObject.get("client_id")));
            sendNotification(msgText.toString());
        }
    }
    
    public String getClientNameByID(long clientId) throws SQLException {
        String clientName = null;
        try{
            clientName = roselServerDAO.getClientName(clientId);
        } catch (Exception ex){
            LOG.log(Level.SEVERE, null, ex);
            throw ex;
        }
        return clientName;
    }
    
    public void sendNotification(String msgText) throws MessagingException {
        Properties p = getEmailProperties();
        p.put("recipient", "OL@rosel.ru, nikiforov.nikita@rosel.ru");
        p.put("subject", "Новый заказ из мобильного приложения");
        p.put("text", msgText);
        sendEmail(p);
    }
    
    public Properties getEmailProperties() {
        Properties emailprops = new Properties();
        emailprops.setProperty("mail.smtp.host", serverSettings.getProperty(ServerSettings.EMAIL_HOST));
        emailprops.setProperty("mail.smtp.port", serverSettings.getProperty(ServerSettings.EMAIL_PORT));
        emailprops.setProperty("from", serverSettings.getProperty(ServerSettings.EMAIL_FROM));
        emailprops.setProperty("login", serverSettings.getProperty(ServerSettings.EMAIL_LOGIN));
        emailprops.setProperty("pwd", serverSettings.getProperty(ServerSettings.EMAIL_PASSWORD));
        return emailprops;
    }
    
    public void sendEmail(Properties p) throws MessagingException {
        Session session = Session.getDefaultInstance(p, null);
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(p.getProperty("from"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(p.getProperty("recipient")));
        msg.setSubject(p.getProperty("subject"));
        msg.setSentDate(new Date());
        msg.setText(p.getProperty("text"));
        Transport.send(msg, p.getProperty("login"), p.getProperty("pwd"));
    }
    
    public synchronized DeviceInfo getDeviceInfo(String device_id) {                                
        
        return roselServerDAO.getDeviceInfo(device_id);        
        
    }

//    public ClientModel checkDevice(String device_id) throws SQLException {        
//        ClientModel clientModel = null;
//        Connection conn = dbManager.getDbConnection();
//        try (Statement stmt = conn.createStatement(); ResultSet res = stmt.executeQuery(getDeviceInfoQuery(device_id))) {
//            if (res.next()) {
//                if (res.getBoolean("confirmed")) {
//                    clientModel = new ClientModel();
//                    clientModel.setId(res.getLong("_id"));
//                    clientModel.setDevice_id(device_id);
//                    clientModel.setManager_id(res.getLong("manager_id"));
//                    clientModel.setName(res.getString("name"));
//                }
//            } else {
//                initializeDevice(device_id);
//            }
//        } catch (SQLException ex) {
//            LOG.log(Level.SEVERE, null, ex);
//            throw ex;
//        }
//        return clientModel;
//    }

//    void initializeDevice(String device_id) throws SQLException {
//        dbManager.executeQuery(getNewDeviceQuery(device_id));
//        long savedID;
//        try (Statement stmt = dbManager.getDbConnection().createStatement(); ResultSet res = stmt.executeQuery(getDeviceInfoQuery(device_id))) {
//            res.next();
//            savedID = res.getLong("_id");
//        } catch(SQLException ex){
//            LOG.log(Level.SEVERE, null, ex);
//            throw ex;
//        }
//        for (String tableName : getVersionTables()) {
//            dbManager.executeQuery(getNewVersionsTablesQuery(tableName, savedID));
//        }
//    }
    
    public void handleConnectionException(Exception ex) {
        LOG.log(Level.SEVERE, "Transport Exception:", ex);   
    }
    
    public void initializeDB(){        
        
        if(transport!=null && transport.isStarted()){
            notifyObservers("Can't initialize! Stop server first!");
            return;
        }
        
        try {
            context = new AnnotationConfigApplicationContext(RoselSpringConf.class);
            roselServerDAO = context.getBean(RoselServerDAO.class);
            roselServerDAO.initializeDataStructure();
            context.registerShutdownHook();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            notifyObservers("Can't initialize! Database problems.");            
            return;
        }           
        notifyObservers("Database initialized!");            
    }
    
}
