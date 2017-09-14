package modules.serverlogic;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import modules.transport.AcceptClientException;
import modules.transport.ServerTransport;
import modules.transport.TransportException;
import modules.transport.TransportMessage;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import roselsalesserver.db.DatabaseManager;

public class RoselServerModel {
    
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
            dbManager.initConnection();
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
            return;
        }
    }
    
    public void stopServer(){        
        if(dbManager!=null){
            dbManager.endWork();
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

    public synchronized TransportMessage handleClientRequest(TransportMessage request, ClientModel clientModel) throws SQLException, ParseException {
        TransportMessage response = new TransportMessage();
        response.setDevice_id(TransportMessage.SERVER_ID);        
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
                postOrders(request.getBody(), clientModel);
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
    
    private ArrayList<String> getUpdates(ClientModel clientModel) throws SQLException {
        ArrayList<String> updates = new ArrayList<String>(0);
        ServerDbItemFactory factory = new ServerDbItemFactory();
        clientModel.setUpdatedTableVersions(getTableVersionsMap());
        HashMap<String, Long> updatedTableVersions = clientModel.getUpdatedTableVersions();

        //int updateSize = 0;
        ResultSet resSet = null;

        try (Statement stmt = dbManager.getDbConnection().createStatement()) {

            //CLIENTS UPDATES                
            //updateSize = resSet.getFetchSize();
            resSet = stmt.executeQuery(getClientsUpdatesQuery(clientModel));
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
            resSet = stmt.executeQuery(getProductsUpdatesQuery(clientModel));

            //updateSize = resSet.getFetchSize();
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
            resSet = stmt.executeQuery(getAddressesUpdatesQuery(clientModel));
            //updateSize = resSet.getFetchSize();

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
            resSet = stmt.executeQuery(getPricesUpdatesQuery(clientModel));
            //updateSize = resSet.getFetchSize();

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
            resSet = stmt.executeQuery(getStockUpdatesQuery(clientModel));
            //updateSize = resSet.getFetchSize();

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
        } catch (SQLException ex){
            throw ex;
        }
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

    private void postOrders(ArrayList<String> ordersInJSON, ClientModel clientModel) throws SQLException, ParseException {
        postOrdersInJSON(ordersInJSON, clientModel);
        if (ordersInJSON.size() > 0) {            
            try {            
                sendNotificationsForOrders(ordersInJSON, clientModel);
            } catch (MessagingException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }        
    }  
    
    public void postOrdersInJSON(ArrayList<String> ordersInJSON, ClientModel clientModel) throws SQLException, ParseException {        
        ResultSet rs = null;
        Connection dbConnection = null;
        Statement stmt = null;
        PreparedStatement pstmt = null;
        String orderDate;
        String shippingDate;
        long order_id;
        String insertQuery;

        JSONParser parser = new JSONParser();
        org.json.simple.JSONObject jsonObject;
        JSONArray lines;
        JSONObject orderLine;

        try {
            dbConnection = dbManager.getDbConnection();             
            stmt = dbConnection.createStatement();            
            for (String jsonString : ordersInJSON) {
                jsonObject = (JSONObject) parser.parse(jsonString);
                if (jsonObject.get("order_date") == null) {
                    orderDate = "null";
                } else {
                    orderDate = "'" + jsonObject.get("order_date").toString() + "'";
                }
                if (jsonObject.get("shipping_date") == null) {
                    shippingDate = "null";
                } else {
                    shippingDate = "'" + jsonObject.get("shipping_date").toString() + "'";
                }
                insertQuery = "INSERT INTO ORDERS (device_id, client_id, order_date, shipping_date, sum, comment, address_id) "
                        + "VALUES ("
                        + clientModel.getId() + ", "
                        + jsonObject.get("client_id") + ", "
                        + orderDate + ", "
                        + shippingDate + ", "
                        + "ROUND(" + jsonObject.get("sum") + ",2), '"
                        + jsonObject.get("comment") + "', "
                        + jsonObject.get("address_id") + ");";
                pstmt = dbConnection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);                
                rs = pstmt.getGeneratedKeys();                
                order_id = rs.getLong(1);

                lines = (JSONArray) jsonObject.get("lines");
                int len = lines.size();
                for (int i = 0; i < len; i++) {
                    orderLine = (JSONObject) lines.get(i);
                    insertQuery = "INSERT INTO ORDERLINES (order_id, product_id, quantity, price, sum) VALUES (" + String.format("%d", order_id) + ", " + orderLine.get("product_id").toString() + ", " + orderLine.get("quantity").toString() + "," + orderLine.get("price").toString() + ", " + orderLine.get("sum").toString() + ");";
                    stmt.execute(insertQuery);
                }
            }            
        } catch (SQLException | ParseException e) {                        
            throw e;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException ignore) {
                }
            }
        }        
    }
    
    public void sendNotificationsForOrders(ArrayList<String> ordersInJSONArrayList, ClientModel clientModel) throws ParseException, SQLException, MessagingException {        
        for (String jsonString : ordersInJSONArrayList) {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
            StringBuilder msgText = new StringBuilder();
            msgText.append("Поступил новый заказ из мобильного приложения");
            if (clientModel.getName() != null && clientModel.getName().length() > 0) {
                msgText.append(" от ");
                msgText.append(clientModel.getName());
            }
            msgText.append('\n').append("Клиент: ").append(getClientNameByID((Long) jsonObject.get("client_id")));
            sendNotification(msgText.toString());
        }
    }
    
    public String getClientNameByID(long clientId) throws SQLException {
        String clientName = null;
        try (Statement stmt = dbManager.getDbConnection().createStatement(); ResultSet result = stmt.executeQuery("SELECT name FROM CLIENTS WHERE _id = " + clientId)) {
            if (result.next()) {
                clientName = result.getString("name");
            }
        } catch(SQLException ex){
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
    
    public synchronized ClientModel buildClientModel(TransportMessage request) throws SQLException {                                
        ClientModel clientModel = null;
        checkDevice(request.getDevice_id(), clientModel);        
        return clientModel;
    }

    public void checkDevice(String device_id, ClientModel clientModel) throws SQLException {
        Connection conn = dbManager.getDbConnection();
        try (Statement stmt = conn.createStatement(); ResultSet res = stmt.executeQuery(getDeviceInfoQuery(device_id))) {
            if (res.next()) {
                if (res.getBoolean("confirmed")) {
                    clientModel = new ClientModel();
                    clientModel.setId(res.getLong("_id"));
                    clientModel.setDevice_id(device_id);
                    clientModel.setManager_id(res.getLong("manager_id"));
                    clientModel.setName(res.getString("name"));
                }
            } else {
                initializeDevice(device_id);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw ex;
        }
    }

    void initializeDevice(String device_id) throws SQLException {
        dbManager.executeQuery(getNewDeviceQuery(device_id));
        long savedID;
        try (Statement stmt = dbManager.getDbConnection().createStatement(); ResultSet res = stmt.executeQuery(getDeviceInfoQuery(device_id))) {
            res.next();
            savedID = res.getLong("_id");
        }
        for (String tableName : getVersionTables()) {
            dbManager.executeQuery(getNewVersionsTablesQuery(tableName, savedID));
        }
    }

    public synchronized void commitClientsUpdate(ClientModel clientModel) {
        HashMap updatedTableVersions = clientModel.getUpdatedTableVersions();
        for (Iterator it = updatedTableVersions.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            if ((Long) entry.getValue() > 0) {
                String updateClientTableVersionsQuery = "UPDATE VERSIONS SET version = " + String.format("%d", entry.getValue()) + " WHERE table_name = '" + (String) entry.getKey() + "' and device_id = '" + clientModel.getId() + "'";
                try {                
                    dbManager.executeQuery(updateClientTableVersionsQuery);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }        
    }    
    
    public void handleConnectionException(Exception ex) {
        LOG.log(Level.SEVERE, "Transport Exception:", ex);   
    }
    
    public void initializeDB(){        
        
        if(transport.isStarted()){
            notifyObservers("Can't initialize! Stop server first!");
            return;
        }
        
        Connection conn;
        try {
            conn = dbManager.getDbConnection();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);            
            notifyObservers("Can't initialize! Database problems.");
            return;
        }
        try (Statement st = conn.createStatement();){
            
            // DELETE TABLES
            st.execute("IF OBJECT_ID('STOCK', 'U') IS NOT NULL DROP TABLE STOCK;");                        
            st.execute("IF OBJECT_ID('ORDERLINES', 'U') IS NOT NULL DROP TABLE ORDERLINES;");
            st.execute("IF OBJECT_ID('ORDERS', 'U') IS NOT NULL DROP TABLE ORDERS;");
            st.execute("IF OBJECT_ID('PRICES', 'U') IS NOT NULL DROP TABLE PRICES;");
            st.execute("IF OBJECT_ID('ADDRESSES', 'U') IS NOT NULL DROP TABLE ADDRESSES");
            st.execute("IF OBJECT_ID('CLIENTS', 'U') IS NOT NULL DROP TABLE CLIENTS;");            
            st.execute("IF OBJECT_ID('PRODUCTS', 'U') IS NOT NULL DROP TABLE PRODUCTS;");
            st.execute("IF OBJECT_ID('VERSIONS', 'U') IS NOT NULL DROP TABLE VERSIONS;");
            st.execute("IF OBJECT_ID('DEVICES', 'U') IS NOT NULL DROP TABLE DEVICES;");
            st.execute("IF OBJECT_ID('MANAGERS', 'U') IS NOT NULL DROP TABLE MANAGERS;");            
            st.execute("IF OBJECT_ID('UPDATES', 'U') IS NOT NULL DROP TABLE UPDATES");
            
            //CREATE TABLES
            st.execute("CREATE TABLE MANAGERS (\n"
                    + "  _id       integer NOT NULL PRIMARY KEY IDENTITY(1,1),\n"
                    + "  rosel_id  nvarchar(100),\n"
                    + "  /* Keys */\n"
                    + "  UNIQUE (rosel_id)\n"
                    + ");");
            st.execute("CREATE TABLE CLIENTS (\n"
                    + "  _id        integer PRIMARY KEY IDENTITY(1,1),\n"
                    + "  rosel_id   nchar(9),\n"
                    + "  name       text,\n"
                    + "  address    text,\n"                    
                    + "  manager_id integer,\n"
                    + "  /* Keys */\n"
                    + "  UNIQUE (rosel_id),\n"
                    + "  /* Foreign keys */\n"
                    + "  CONSTRAINT CLIENTS_Foreign_key01\n"
                    + "    FOREIGN KEY (manager_id)\n"
                    + "    REFERENCES MANAGERS(_id)\n"
                    + ");");
            st.execute("CREATE TABLE ADDRESSES (\n"
                    + "  _id       integer PRIMARY KEY IDENTITY(1,1),\n"
                    + "  rosel_id  nchar(11),\n"
                    + "  client_id integer,\n"
                    + "  address   text,\n"
                    + "  /* Keys */\n"
                    + "  UNIQUE (rosel_id),\n"
                    + "  /* Foreign keys */\n"
                    + "  CONSTRAINT ADDRESSES_Foreign_key01\n"
                    + "    FOREIGN KEY (client_id)\n"
                    + "    REFERENCES CLIENTS(_id), \n"
                    + ");");            
            st.execute("CREATE TABLE PRODUCTS (\n"
                    + "  _id       integer PRIMARY KEY IDENTITY(1,1),\n"
                    + "  rosel_id  nchar(11),\n"
                    + "  code      text,\n"
                    + "  name      text,\n"
                    + "  isgroup   integer,\n"
                    + "  group_id  text,\n"
                    + "  show      integer,\n"
                    + "  /* Keys */\n"
                    + "  UNIQUE (rosel_id)\n"
                    + ");");
            st.execute("CREATE TABLE DEVICES (\n"
                    + "  _id         integer PRIMARY KEY IDENTITY(1,1),\n"
                    + "  device_id   nvarchar(50),\n"
                    + "  name        text,\n"
                    + "  manager_id  integer,\n"
                    + "  /* Keys */\n"
                    + "  UNIQUE (device_id),\n"
                    + "  /* Foreign keys */\n"
                    + "  CONSTRAINT DEVICES_Foreign_key01\n"
                    + "    FOREIGN KEY (manager_id)\n"
                    + "    REFERENCES MANAGERS(_id)\n"
                    + ");");
            st.execute("CREATE TABLE ORDERS (\n"
                    + "  _id         integer PRIMARY KEY IDENTITY(1,1),\n"
                    + "  client_id   integer,\n"
                    + "  device_id   integer,\n"
                    + "  rosel_id    nchar(11),\n"
                    + "  address_id  integer,\n"
                    + "  order_date  text,\n"
                    + "  shipping_date  text,\n"
                    + "  comment  text,\n"
                    + "  \"sum\"       real,\n"
                    + "  /* Foreign keys */\n"
                    + "  CONSTRAINT ORDERS_Foreign_key01\n"
                    + "    FOREIGN KEY (client_id)\n"
                    + "    REFERENCES CLIENTS(_id), \n"
                    + "  CONSTRAINT ORDERS_Foreign_key02\n"
                    + "    FOREIGN KEY (device_id)\n"
                    + "    REFERENCES DEVICES(_id),\n"
                    + "  CONSTRAINT ORDERS_Foreign_key03\n"
                    + "    FOREIGN KEY (address_id)\n"
                    + "    REFERENCES ADDRESSES(_id)\n"
                    + ");");
            st.execute("CREATE TABLE ORDERLINES (\n"
                    + "  _id         integer PRIMARY KEY IDENTITY(1,1),\n"
                    + "  order_id    integer,\n"
                    + "  product_id  integer,\n"
                    + "  quantity    integer,\n"
                    + "  price       real,\n"
                    + "  \"sum\"       real,\n"
                    + "  /* Foreign keys */\n"
                    + "  CONSTRAINT ORDERLINES_Foreign_key01\n"
                    + "    FOREIGN KEY (product_id)\n"
                    + "    REFERENCES PRODUCTS(_id), \n"
                    + "  CONSTRAINT ORDERLINES_Foreign_key02\n"
                    + "    FOREIGN KEY (order_id)\n"
                    + "    REFERENCES ORDERS(_id)\n"
                    + ");");            
            st.execute("CREATE TABLE PRICES (\n"
                    + "  _id         integer PRIMARY KEY IDENTITY(1,1),\n"
                    + "  product_id  integer,\n"
                    + "  client_id   integer,\n"
                    + "  price       real,\n"
                    + "  /* Keys */\n"
                    + "  UNIQUE (product_id, client_id),\n"
                    + "  /* Foreign keys */\n"
                    + "  CONSTRAINT PRICES_Foreign_key02\n"
                    + "    FOREIGN KEY (product_id)\n"
                    + "    REFERENCES PRODUCTS(_id), \n"
                    + "  CONSTRAINT PRICES_Foreign_key01\n"
                    + "    FOREIGN KEY (client_id)\n"
                    + "    REFERENCES CLIENTS(_id)\n"
                    + ");");            
            st.execute("CREATE TABLE STOCK (\n"
                    + "  _id         integer PRIMARY KEY IDENTITY(1,1),\n"
                    + "  product_id  integer,\n"
                    + "  quantity    real,\n"
                    + "  /* Keys */\n"
                    + "  UNIQUE (product_id),\n"
                    + "  /* Foreign keys */\n"
                    + "  CONSTRAINT STOCK_Foreign_key01\n"
                    + "    FOREIGN KEY (product_id)\n"
                    + "    REFERENCES PRODUCTS(_id)\n"
                    + ");");
            st.execute("CREATE TABLE UPDATES (\n"
                    + "  _id         integer NOT NULL PRIMARY KEY IDENTITY(1,1),\n"
                    + "  item_id     integer NOT NULL,\n"
                    + "  table_name  nvarchar(20),\n"
                    + "  \"action\"    integer,\n"
                    + "  version     nchar(11),\n"                    
                    + ");");
            st.execute("CREATE TABLE VERSIONS (\n"
                    + "  _id         integer PRIMARY KEY IDENTITY(1,1),\n"
                    + "  table_name  nvarchar(20),\n"
                    + "  version     integer,\n"
                    + "  device_id   integer,\n"
                    + "  /* Keys */\n"
                    + "  UNIQUE (table_name, device_id),\n"
                    + "  /* Foreign keys */\n"
                    + "  CONSTRAINT VERSIONS_Foreign_key01\n"
                    + "    FOREIGN KEY (device_id)\n"
                    + "    REFERENCES DEVICES(_id)\n"
                    + ");");
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            notifyObservers("Can't initialize! Database problems.");            
            return;
        }           
        notifyObservers("Database initialized!");            
    }
    
}
