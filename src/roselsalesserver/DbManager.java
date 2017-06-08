package roselsalesserver;

import modules.serverlogic.ServerDbItemFactory;
import modules.serverlogic.DbItem;
import modules.serverlogic.ServerSettings;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DbManager {
    
    static final String JDBC_DRIVER_POSTGRE = "org.postgresql.Driver";    
    static final String JDBC_DRIVER_SQLITE = "org.sqlite.JDBC";    
    static final String JDBC_DRIVER_SQL = "com.microsoft.sqlserver.jdbc.SQLServerDriver";       
    
    private String driverName;
    private String dbUrl;
    private Connection dbConnection = null;
    private Statement stmt = null;
    private static Logger log = Logger.getLogger(DbManager.class.getName());
    
    public DbManager(Properties settings) {
        switch (settings.getProperty(ServerSettings.DB_TYPE).toString()) {
            case "MS SQL Server":
                this.driverName = JDBC_DRIVER_SQL;
                this.dbUrl = "jdbc:sqlserver://" + settings.getProperty(ServerSettings.DB_SERVER).toString() + ";databaseName=" + settings.getProperty(ServerSettings.DB_NAME).toString();
                break;
            case "PostgreSQL":
                this.driverName = JDBC_DRIVER_POSTGRE;
                this.dbUrl = "jdbc:postgresql://" + settings.getProperty(ServerSettings.DB_SERVER).toString() + "/" + settings.getProperty(ServerSettings.DB_NAME).toString();
                break;
            case "SQLite":
                this.driverName = JDBC_DRIVER_SQLITE;
                this.dbUrl = "jdbc:sqlite://" + settings.getProperty(ServerSettings.DB_NAME).toString();
                break;
        }        
        
        connect(settings.getProperty(ServerSettings.DB_LOGIN).toString(), settings.getProperty(ServerSettings.DB_PASSWORD).toString());
    }
    
    public final boolean connect(String user, String pwd) {
        if (!isConnected()) {
            try {
                Class.forName(driverName);
                dbConnection = DriverManager.getConnection(dbUrl, user, pwd);
            } catch (Exception ex) {
                log.log(Level.SEVERE,  "Exception: ", ex);                
                return false;
            }
        }
        return true;
    }

    public boolean isConnected() {
        return dbConnection != null;
    }
    
    public boolean initializeDB() {        
        try {
            //drop tables            
            Statement st = dbConnection.createStatement();

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
            st.execute("CREATE TABLE CLIENTS (\n"
                    + "  _id       integer PRIMARY KEY IDENTITY(1,1),\n"
                    + "  rosel_id  nchar(9),\n"
                    + "  name      text,\n"
                    + "  address   text,\n"                    
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
            st.execute("CREATE TABLE MANAGERS (\n"
                    + "  _id       integer NOT NULL PRIMARY KEY IDENTITY(1,1),\n"
                    + "  rosel_id  nvarchar(100),\n"
                    + "  /* Keys */\n"
                    + "  UNIQUE (rosel_id)\n"
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
            log.log(Level.SEVERE,  "Exception: ", ex);
            return false;
        }   
        return true;
    }
    
    public boolean clientExist(String deviceID) throws SQLException {
        Statement st = dbConnection.createStatement();
        String deviceCheckQuery = "SELECT _id, device_id, name FROM DEVICES WHERE device_id = '" + deviceID + "'";
        ResultSet result = st.executeQuery(deviceCheckQuery);        
        return result.next();        
    }
    
    public MobileClient getMobileClientByID(String deviceID) throws SQLException {
        Statement st = dbConnection.createStatement();
        String deviceCheckQuery = "SELECT _id, device_id, name, CASE ISNULL(manager_id,0) WHEN 0 THEN 0 ELSE 1 END AS confirmed FROM DEVICES WHERE device_id = '" + deviceID + "'";
        ResultSet result = st.executeQuery(deviceCheckQuery);        
        if(result.next()){
            MobileClient mc = new MobileClient(result.getLong("_id"), deviceID);
            mc.setName(result.getString("name"));
            mc.setConfirmed(result.getBoolean("confirmed"));
            return mc;
        } else {
            return null;        
        }        
    }
    
    public String getClientNameByID(long clientId){
        try{
        Statement st = dbConnection.createStatement();
        String deviceCheckQuery = "SELECT name FROM CLIENTS WHERE _id = " + clientId;
        ResultSet result = st.executeQuery(deviceCheckQuery);        
        if(result.next()){            
            return result.getString("name");
        } else {
            return null;        
        }
        }catch(Exception ex){
            return null;
        }
    }
    
    public void initializeDevice(String deviceID) throws SQLException{
        Statement st = dbConnection.createStatement();
        //String newDeviceQuery = "INSERT OR IGNORE INTO DEVICES (device_id) VALUES ('" + deviceID + "')";
        String newDeviceQuery = "IF NOT EXISTS (SELECT device_id from DEVICES WHERE device_id = '" + deviceID + "') "                
                + "INSERT INTO DEVICES (device_id) VALUES ('" + deviceID + "');";
        st.execute(newDeviceQuery);        
        
        newDeviceQuery = "SELECT _id FROM DEVICES WHERE device_id = '" + deviceID + "'";
        ResultSet res = st.executeQuery(newDeviceQuery);        
        res.next();        
        long savedID = res.getLong("_id");

        String nullTableVersionsQuery;
        ArrayList<String> versionTables = getVersionTables();
        for(String tableName:versionTables){
            //nullTableVersionsQuery = "INSERT OR REPLACE INTO VERSIONS (device_id, table_name, version) VALUES ("+ String.format("%d", savedID) +", '"+ tableName +"', 0)";
            nullTableVersionsQuery = "IF NOT EXISTS (SELECT device_id from VERSIONS WHERE device_id = " + String.format("%d", savedID) + " AND table_name = '"+ tableName +"')"
                + "INSERT INTO VERSIONS (device_id, table_name, version) VALUES ("+ String.format("%d", savedID) +", '"+ tableName +"', 0)"                
                + "ELSE "
                    + "UPDATE VERSIONS SET version = 0 WHERE device_id = " + String.format("%d", savedID) + " AND table_name = '"+ tableName +"';";                    
            st.execute(nullTableVersionsQuery);
        }        
    }
    
    public ArrayList<String> getVersionTables(){
        ArrayList<String> tables = new ArrayList();
        tables.add("PRODUCTS");
        tables.add("CLIENTS");
        tables.add("PRICES"); 
        //tables.add("STOCK"); usage of update table canceled
        tables.add("ADDRESSES");
        return tables;
    }
    
    public ArrayList<JSONObject> getUpdatesForClient(MobileClient mobileClient) throws SQLException {
        ArrayList<DbItem> itemsList = new ArrayList<>();
        ArrayList<JSONObject> jsonres = new ArrayList<>();
        ArrayList<String> versionTables = getVersionTables();
        HashMap updatedTableVersions = mobileClient.getUpdatedTableVersions();
        for (String tableName : versionTables) {
            updatedTableVersions.put(tableName, new Long(0));
        }
        ServerDbItemFactory factory = new ServerDbItemFactory();
        String getUpdatesQuery = "SELECT "
                + "UPDATES.item_id AS _id, "
                + "UPDATES.table_name AS table_name, "
                + "MAX(UPDATES._id) AS version, "
                + "MIN(UPDATES.action) AS action "
                + "FROM "
                + "VERSIONS "
                + "INNER JOIN UPDATES ON (VERSIONS.version < UPDATES._id) "
                + "AND (VERSIONS.table_name = UPDATES.table_name) "
                + "WHERE "
                + "VERSIONS.device_id = " + mobileClient.getId() + " "
                + "AND UPDATES.table_name <> 'PRICES' "
                + "AND UPDATES.table_name <> 'CLIENTS' "
                + "AND UPDATES.table_name <> 'ADDRESSES' "
                + "GROUP BY "
                + "UPDATES.table_name, "
                + "UPDATES.item_id;";        
        Statement st = dbConnection.createStatement();
        ResultSet resSet;
        ResultSet updatesResSet = st.executeQuery(getUpdatesQuery);        
        while(updatesResSet.next()){            
            String tableName = updatesResSet.getString("table_name");
            String _id = String.format("%d",updatesResSet.getLong("_id"));
            String version = String.format("%d",updatesResSet.getLong("version"));
            String action = String.format("%d",updatesResSet.getLong("action"));                        
            String getItemsForUpdateQuery = "SELECT " + action + " AS action, * FROM " + tableName + " WHERE _id = " + _id;                    
            resSet = dbConnection.createStatement().executeQuery(getItemsForUpdateQuery);            
            while (resSet.next()) {
                itemsList.add(factory.fillFromResultSet(resSet, tableName));
            }                        
            if((Long) updatedTableVersions.get(tableName) < Long.parseLong(version)){
                updatedTableVersions.put(tableName,Long.parseLong(version));
            }            
        }      
        
        //CLIENTS UPDATES
        String getClientsQuery = "SELECT temp.version, temp.action, C.* FROM " +
	"(SELECT " +
	"	U.item_id," +  
	"	MAX(U._id) AS version," +  
	"	MIN(U.action) AS action," + 
	"	V.device_id " +
	"FROM VERSIONS AS V" + 
	"	INNER JOIN UPDATES AS U ON (V.version < U._id) AND V.table_name = 'CLIENTS' AND V.device_id = " + mobileClient.getId() + " AND U.table_name = 'CLIENTS' " + 
	"GROUP BY U.item_id, V.device_id) AS temp " + 
        "       INNER JOIN CLIENTS AS C ON C._id = temp.item_id " + 
        "       INNER JOIN DEVICES AS D ON C.manager_id = D.manager_id AND temp.device_id = D._id;";        
        resSet = st.executeQuery(getClientsQuery);
        while (resSet.next()) {
            itemsList.add(factory.fillFromResultSet(resSet, "CLIENTS", 2));
            long lastVersion = (long) updatedTableVersions.get("CLIENTS");
            if(lastVersion < resSet.getLong("version")){
                lastVersion = resSet.getLong("version");
                updatedTableVersions.put("CLIENTS", lastVersion);
            }
            updatedTableVersions.put("CLIENTS", lastVersion);
        }
        
        //ADDRESSES UPDATES
        String getAdresssesQuery = "SELECT temp.version, temp.action, A.* FROM " +
	"(SELECT " +
	"	U.item_id," +  
	"	MAX(U._id) AS version," +  
	"	MIN(U.action) AS action," + 
	"	V.device_id " +
	"FROM VERSIONS AS V" + 
	"	INNER JOIN UPDATES AS U ON (V.version < U._id) AND V.table_name = 'ADDRESSES' AND V.device_id = " + mobileClient.getId() + " AND U.table_name = 'ADDRESSES' " + 
	"GROUP BY U.item_id, V.device_id) AS temp " + 
	"       INNER JOIN ADDRESSES AS A ON A._id = temp.item_id " + 
        "       INNER JOIN CLIENTS AS C ON A.client_id = C._id " + 
        "       INNER JOIN DEVICES AS D ON C.manager_id = D.manager_id AND temp.device_id = D._id;";        
        resSet = st.executeQuery(getAdresssesQuery);
        while (resSet.next()) {
            itemsList.add(factory.fillFromResultSet(resSet, "ADDRESSES", 2));
            long lastVersion = (long) updatedTableVersions.get("ADDRESSES");
            if(lastVersion < resSet.getLong("version")){
                lastVersion = resSet.getLong("version");
                updatedTableVersions.put("ADDRESSES", lastVersion);
            }
            updatedTableVersions.put("ADDRESSES", lastVersion);
        }
        
        //PRICES UPDATES        
        String getPricesQuery = "SELECT temp.version, temp.action, P.* FROM " +
	"(SELECT " +
	"	U.item_id," +  
	"	MAX(U._id) AS version," +  
	"	MIN(U.action) AS action," + 
	"	V.device_id " +
	"FROM VERSIONS AS V" + 
	"	INNER JOIN UPDATES AS U ON (V.version < U._id) AND V.table_name = 'PRICES' AND V.device_id = " + mobileClient.getId() + " AND U.table_name = 'PRICES' " + 
	"GROUP BY U.item_id, V.device_id) AS temp " + 
	"       INNER JOIN PRICES AS P ON P._id = temp.item_id " + 
        "       INNER JOIN CLIENTS AS C ON P.client_id = C._id " + 
        "       INNER JOIN DEVICES AS D ON C.manager_id = D.manager_id AND temp.device_id = D._id;";        
        resSet = st.executeQuery(getPricesQuery);
        while (resSet.next()) {
            itemsList.add(factory.fillFromResultSet(resSet, "PRICES", 2));
            long lastVersion = (long) updatedTableVersions.get("PRICES");
            if(lastVersion < resSet.getLong("version")){
                lastVersion = resSet.getLong("version");
                updatedTableVersions.put("PRICES", lastVersion);
            }
            updatedTableVersions.put("PRICES", lastVersion);
        }
        
        //STOCK UPDATES
        String getStockQuery = "SELECT " + DbItem.ACTION_NEW + " AS action, STOCK.* FROM STOCK";
        resSet = st.executeQuery(getStockQuery);
        while (resSet.next()) {
            itemsList.add(factory.fillFromResultSet(resSet, "STOCK"));
        }                
        
        //write updates to JSON
        for (DbItem dbitem : itemsList) {
            jsonres.add(dbitem.toJSONObject());
        }        
        return jsonres;
    }
    
    public void updateClientVersions(MobileClient mobileClient) throws SQLException{
        HashMap updatedTableVersions = mobileClient.getUpdatedTableVersions();
        for (Iterator it = updatedTableVersions.entrySet().iterator(); it.hasNext(); ) {            
            Map.Entry entry = (Map.Entry) it.next();            
            String tableName = (String) entry.getKey();            
            if((Long) entry.getValue()>0){
                String newVersion = String.format("%d", entry.getValue());
                String updateClientTableVersionsQuery = "UPDATE VERSIONS SET version = " + newVersion + " WHERE table_name = '" + tableName + "' and device_id = " + mobileClient.getId();
                Statement st = dbConnection.createStatement();
                st.executeUpdate(updateClientTableVersionsQuery);
            }
        }        
    }
    
    public boolean postDbItems(ArrayList<DbItem> dbItems) {        
        try {
            dbConnection.setAutoCommit(false);
            stmt = dbConnection.createStatement();
            for (DbItem dbItem : dbItems) {
                if (dbItem != null) {
                    switch (dbItem.action) {
                        case DbItem.ACTION_NEW:
                            //insert new row in DB                            
                            StringBuilder columnNames = new StringBuilder();
                            StringBuilder values = new StringBuilder();
                            for (DbItem.ItemValue iv : dbItem.item_values) {
                                if (!iv.value.equals("null")&&!iv.name.equals("_id")) {                                    
                                    columnNames.append("," + iv.name);
                                    switch (iv.type) {
                                        case "INTEGER":
                                            values.append("," + Long.valueOf(iv.value));
                                            break;
                                        case "TEXT":
                                            values.append(",'" + iv.value + "'");
                                            break;
                                        case "REAL":
                                            values.append("," + Float.valueOf(iv.value));
                                            break;
                                        default:
                                            values.append(",'" + iv.value.toString() + "'");
                                            break;
                                    }
                                }
                            }                            
                            String sql = "INSERT INTO " + dbItem.table_name + " (_id " + columnNames.toString() + ") "
                                    + "VALUES (" + String.format("%d", dbItem.id) + values.toString() + ");";                            
                            stmt.executeUpdate(sql);
                            break;
                    }
                } else {                    
                    log.log(Level.SEVERE,  "Met situation when DbItem is NULL!");
                    return false;
                }
            }
            dbConnection.commit();            
        } catch (SQLException | NumberFormatException ex) {
            log.log(Level.SEVERE, "Exception: ", ex);
            return false;
        } finally {
            try {
                if (!dbConnection.getAutoCommit()) {
                    dbConnection.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                log.log(Level.SEVERE, "Exception: ", ex);
                return false;
            }
        }
        return true;
    }
    
    public boolean postOrdersInJSON(ArrayList<String> ordersInJSON, MobileClient mobileClient) {        
        try {
            dbConnection.setAutoCommit(false);
            stmt = dbConnection.createStatement();
            for (String jsonString : ordersInJSON) {
                JSONParser parser = new JSONParser();                
                JSONObject jsonObject = (JSONObject) parser.parse(jsonString);                                
                String orderDate;
                String shippingDate;
                if(jsonObject.get("order_date")==null){
                  orderDate = "null";  
                } else {
                  orderDate = "'" + jsonObject.get("order_date").toString() + "'";    
                };                
                if(jsonObject.get("shipping_date")==null){
                  shippingDate = "null";  
                } else {
                  shippingDate = "'" + jsonObject.get("shipping_date").toString() + "'";    
                };
                String sql_insert = "INSERT INTO ORDERS (device_id, client_id, order_date, shipping_date, sum, comment, address_id) "
                        + "VALUES (" 
                        + mobileClient.getId() + ", " 
                        + jsonObject.get("client_id") + ", " 
                        //+ jsonObject.get("order_date") + "', "
                        + orderDate + ", " 
                        //+ jsonObject.get("shipping_date") + "', " 
                        + shippingDate + ", " 
                        + "ROUND(" + jsonObject.get("sum") + ",2), '" 
                        + jsonObject.get("comment") + "', " 
                        + jsonObject.get("address_id") + ");";
                PreparedStatement statement = dbConnection.prepareStatement(sql_insert, Statement.RETURN_GENERATED_KEYS);
                if (statement.executeUpdate() == 0) {
                    return false;
                };
                ResultSet rs = statement.getGeneratedKeys();                
                if (!rs.next()) {                    
                    return false;
                }
                long order_id = rs.getLong(1);

                JSONArray lines = (JSONArray) jsonObject.get("lines");
                int len = lines.size();
                for (int i = 0; i < len; i++) {
                    JSONObject orderLine = (JSONObject) lines.get(i);
                    sql_insert = "INSERT INTO ORDERLINES (order_id, product_id, quantity, price, sum) VALUES (" + String.format("%d", order_id) + ", " + orderLine.get("product_id").toString() + ", " + orderLine.get("quantity").toString() + "," + orderLine.get("price").toString() + ", " + orderLine.get("sum").toString() + ");";
                    stmt.execute(sql_insert);
                }
            }
            dbConnection.commit();            
        } catch (SQLException | ParseException e) {
            try {
                dbConnection.rollback();
            } catch (SQLException ex) {
                log.log(Level.SEVERE, "Exception: ", ex);
            }
            log.log(Level.SEVERE, "Exception: ", e);
            return false;
        } finally {
            try {
                if (!dbConnection.getAutoCommit()) {
                    dbConnection.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                log.log(Level.SEVERE, "Exception: ", ex);
                return false;
            }
        }
        return true;
    }
    
    public void disconnect() {
        if (dbConnection != null) {
            try {
                dbConnection.close();                
            } catch (Exception ex) {
                log.log(Level.SEVERE, "Exception: ", ex);
            }
        }
    }
    
}
