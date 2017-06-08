package roselsalesserver;

import modules.serverlogic.ServerDbItemFactory;
import modules.serverlogic.DbItem;
import modules.serverlogic.ServerSettings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import roselsalesserver.UI.ServerPanel;
import roselsalesserver.db.DatabaseManager;

public class RoselSalesServer {

    ServerSocket serverSocket;
    JFrame mainFrame;
    JPanel mainPanel;
    Properties serverSettings;
    private static final Logger log = Logger.getLogger(RoselSalesServer.class.getName());

    public static final int SERVER_SOCKET = 60611;

    void buildUI() {
        mainFrame = new JFrame("Rosel server");
        mainPanel = new ServerPanel(this);
        mainFrame.getContentPane().add(mainPanel);
        mainFrame.pack();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);
    }

    class ClientHandler extends Thread {

        Socket clientSocket;
        MobileClient mobileClient;
        int clientIntention = 0; //describes what client wants from server side        
        Messenger messenger;
        DatabaseManager databaseManager;

        public ClientHandler(Socket clientSocket) throws IOException, Exception {
            this.clientSocket = clientSocket;
            messenger = new Messenger(this);
            databaseManager = DatabaseManager.getDatabaseManager(serverSettings);
        }

        @Override
        public void run() {

            try {
                if (!checkClient()) {
                    log.info("Client check problem");
                    return;
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, "Exception: ", ex);
                close();
                return;
            }

            if (!clientIsConfirmed()) {
                close();
                return;
            }

            //client checked
            try {
                checkIntention();
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Exception: ", ex);
                close();
                return;
            }

            try {
                handleIntention();
            } catch (Exception ex) {
                log.log(Level.SEVERE, "Exception: ", ex);
                close();
                return;
            }

            close();
        }

        private void close() {
            try {
                databaseManager.endWork();
                clientSocket.close();                
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Exception: ", ex);
            }
        }

        class Messenger {

            ClientHandler clientHandler;
            BufferedReader reader;
            PrintWriter writer;

            public Messenger(ClientHandler clientHandler) throws IOException {
                this.clientHandler = clientHandler;
                writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            }

            String readResponse() throws IOException {
                String responseLine = reader.readLine();
                if (responseLine == null) {
                    responseLine = "";
                }
                return responseLine;
            }

            String askDeviceID() throws IOException {
                writer.println(ExchangeProtocol.DEVICE_ID_REQUEST);
                writer.flush();
                return readResponse();
            }

            String askIntention() throws IOException {
                writer.println(ExchangeProtocol.INTENTION_REQUEST);
                writer.flush();
                return readResponse();
            }

            boolean askConfirmation() throws IOException {
                writer.println(ExchangeProtocol.CONFIRMATION_REQUEST);
                writer.flush();
                return readResponse().equals(ExchangeProtocol.OK_RESPONSE);
            }

            void sendConfirmation() {
                writer.println(ExchangeProtocol.OK_RESPONSE);
                writer.flush();
            }

            void sendDeviceConfirmation() {
                writer.println(ExchangeProtocol.DEVICE_CONFIRMATION);
                writer.flush();
            }

            void sendDeviceRejection() {
                writer.println(ExchangeProtocol.DEVICE_REJECTION);
                writer.flush();
            }

            boolean sendUpdates() {
                ArrayList<JSONObject> updatesList = new ArrayList<>();

                Statement st = null;
                ResultSet resSet = null;
                ResultSet updatesResSet = null;

                try {
                    Connection dbConnection = databaseManager.getDbConnection();
                    ArrayList<DbItem> itemsList = new ArrayList<>();
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
                    st = dbConnection.createStatement();

                    updatesResSet = st.executeQuery(getUpdatesQuery);

                    String tableName;
                    String version;
                    String getItemsForUpdateQuery;
                    while (updatesResSet.next()) {
                        tableName = updatesResSet.getString("table_name");
                        version = String.format("%d", updatesResSet.getLong("version"));
                        getItemsForUpdateQuery = "SELECT " + String.format("%d", updatesResSet.getLong("action")) + " AS action, * FROM " + tableName + " WHERE _id = " + String.format("%d", updatesResSet.getLong("_id"));
                        Statement innerSt = dbConnection.createStatement();
                        resSet = innerSt.executeQuery(getItemsForUpdateQuery);
                        while (resSet.next()) {
                            itemsList.add(factory.fillFromResultSet(resSet, tableName));
                        }
                        if ((Long) updatedTableVersions.get(tableName) < Long.parseLong(version)) {
                            updatedTableVersions.put(tableName, Long.parseLong(version));
                        }
                        innerSt.close();
                    }

                    //CLIENTS UPDATES
                    String getClientsQuery = "SELECT temp.version, temp.action, C.* FROM "
                            + "(SELECT "
                            + "	U.item_id,"
                            + "	MAX(U._id) AS version,"
                            + "	MIN(U.action) AS action,"
                            + "	V.device_id "
                            + "FROM VERSIONS AS V"
                            + "	INNER JOIN UPDATES AS U ON (V.version < U._id) AND V.table_name = 'CLIENTS' AND V.device_id = " + mobileClient.getId() + " AND U.table_name = 'CLIENTS' "
                            + "GROUP BY U.item_id, V.device_id) AS temp "
                            + "       INNER JOIN CLIENTS AS C ON C._id = temp.item_id "
                            + "       INNER JOIN DEVICES AS D ON C.manager_id = D.manager_id AND temp.device_id = D._id;";
                    resSet = st.executeQuery(getClientsQuery);
                    while (resSet.next()) {
                        itemsList.add(factory.fillFromResultSet(resSet, "CLIENTS", 2));
                        long lastVersion = (long) updatedTableVersions.get("CLIENTS");
                        if (lastVersion < resSet.getLong("version")) {
                            lastVersion = resSet.getLong("version");
                            updatedTableVersions.put("CLIENTS", lastVersion);
                        }
                        updatedTableVersions.put("CLIENTS", lastVersion);
                    }

                    //ADDRESSES UPDATES
                    String getAdresssesQuery = "SELECT temp.version, temp.action, A.* FROM "
                            + "(SELECT "
                            + "	U.item_id,"
                            + "	MAX(U._id) AS version,"
                            + "	MIN(U.action) AS action,"
                            + "	V.device_id "
                            + "FROM VERSIONS AS V"
                            + "	INNER JOIN UPDATES AS U ON (V.version < U._id) AND V.table_name = 'ADDRESSES' AND V.device_id = " + mobileClient.getId() + " AND U.table_name = 'ADDRESSES' "
                            + "GROUP BY U.item_id, V.device_id) AS temp "
                            + "       INNER JOIN ADDRESSES AS A ON A._id = temp.item_id "
                            + "       INNER JOIN CLIENTS AS C ON A.client_id = C._id "
                            + "       INNER JOIN DEVICES AS D ON C.manager_id = D.manager_id AND temp.device_id = D._id;";
                    resSet = st.executeQuery(getAdresssesQuery);
                    while (resSet.next()) {
                        itemsList.add(factory.fillFromResultSet(resSet, "ADDRESSES", 2));
                        long lastVersion = (long) updatedTableVersions.get("ADDRESSES");
                        if (lastVersion < resSet.getLong("version")) {
                            lastVersion = resSet.getLong("version");
                            updatedTableVersions.put("ADDRESSES", lastVersion);
                        }
                        updatedTableVersions.put("ADDRESSES", lastVersion);
                    }

                    //PRICES UPDATES        
                    String getPricesQuery = "SELECT temp.version, temp.action, P.* FROM "
                            + "(SELECT "
                            + "	U.item_id,"
                            + "	MAX(U._id) AS version,"
                            + "	MIN(U.action) AS action,"
                            + "	V.device_id "
                            + "FROM VERSIONS AS V"
                            + "	INNER JOIN UPDATES AS U ON (V.version < U._id) AND V.table_name = 'PRICES' AND V.device_id = " + mobileClient.getId() + " AND U.table_name = 'PRICES' "
                            + "GROUP BY U.item_id, V.device_id) AS temp "
                            + "       INNER JOIN PRICES AS P ON P._id = temp.item_id "
                            + "       INNER JOIN CLIENTS AS C ON P.client_id = C._id "
                            + "       INNER JOIN DEVICES AS D ON C.manager_id = D.manager_id AND temp.device_id = D._id;";
                    resSet = st.executeQuery(getPricesQuery);
                    while (resSet.next()) {
                        itemsList.add(factory.fillFromResultSet(resSet, "PRICES", 2));
                        long lastVersion = (long) updatedTableVersions.get("PRICES");
                        if (lastVersion < resSet.getLong("version")) {
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
                        updatesList.add(dbitem.toJSONObject());
                    }
                } catch (SQLException ex) {
                    log.log(Level.SEVERE, "Exception: ", ex);
                    return false;
                } catch (Exception ex) {
                    log.log(Level.SEVERE, "Exception: ", ex);
                    return false;
                } finally {
                    if (resSet != null) {
                        try {
                            resSet.close();
                        } catch (SQLException ex) {
                        }
                    }
                    if (updatesResSet != null) {
                        try {
                            updatesResSet.close();
                        } catch (SQLException ex) {
                        }
                    }
                    if (st != null) {
                        try {
                            st.close();
                        } catch (SQLException ex) {
                        }
                    }
                }

                for (JSONObject curUpdate : updatesList) {
                    writer.println(curUpdate.toJSONString());
                    writer.flush();
                }

                //ask if all data was succesfully updated on client                
                try {
                    return askConfirmation();
                } catch (IOException ex) {
                    log.log(Level.SEVERE, "Exception: ", ex);
                    return false;
                }
            }

            boolean getOrders() throws ParseException, AddressException {
                String line;
                ArrayList<String> ordersInJSON = new ArrayList<>();
                try {
                    while ((line = reader.readLine()) != null && !line.equals(ExchangeProtocol.CONFIRMATION_REQUEST)) {
                        ordersInJSON.add(line);
                    }
                    if (!line.equals(ExchangeProtocol.CONFIRMATION_REQUEST)) {
                        return false;
                    }

                    if (!postOrdersInJSON(ordersInJSON)) {
                        return false;
                    }

                } catch (IOException ex) {
                    log.log(Level.SEVERE, "Exception: ", ex);
                    return false;
                }
                sendConfirmation();
                if (ordersInJSON.size() > 0) {
                    //sendNotifications(ordersInJSON);
                    sendNotificationsForOrders(ordersInJSON);
                }
                return true;
            }
        }

        public ArrayList<String> getVersionTables() {
            ArrayList<String> tables = new ArrayList();
            tables.add("PRODUCTS");
            tables.add("CLIENTS");
            tables.add("PRICES");
            //tables.add("STOCK"); usage of update table canceled
            tables.add("ADDRESSES");
            return tables;
        }

        public boolean postOrdersInJSON(ArrayList<String> ordersInJSON) {
            Connection dbConnection = null;
            PreparedStatement statement = null;
            Statement stmt = null;
            ResultSet rs = null;

            String orderDate;
            String shippingDate;
            long order_id;

            String sql_insert;

            JSONParser parser = new JSONParser();
            JSONObject jsonObject;
            JSONArray lines;
            JSONObject orderLine;

            try {
                dbConnection = databaseManager.getDbConnection();
                dbConnection.setAutoCommit(false);
                stmt = dbConnection.createStatement();

                for (String jsonString : ordersInJSON) {
                    jsonObject = (JSONObject) parser.parse(jsonString);
                    if (jsonObject.get("order_date") == null) {
                        orderDate = "null";
                    } else {
                        orderDate = "'" + jsonObject.get("order_date").toString() + "'";
                    };
                    if (jsonObject.get("shipping_date") == null) {
                        shippingDate = "null";
                    } else {
                        shippingDate = "'" + jsonObject.get("shipping_date").toString() + "'";
                    };
                    sql_insert = "INSERT INTO ORDERS (device_id, client_id, order_date, shipping_date, sum, comment, address_id) "
                            + "VALUES ("
                            + mobileClient.getId() + ", "
                            + jsonObject.get("client_id") + ", "
                            + orderDate + ", "
                            + shippingDate + ", "
                            + "ROUND(" + jsonObject.get("sum") + ",2), '"
                            + jsonObject.get("comment") + "', "
                            + jsonObject.get("address_id") + ");";
                    statement = dbConnection.prepareStatement(sql_insert, Statement.RETURN_GENERATED_KEYS);
                    if (statement.executeUpdate() == 0) {
                        return false;
                    };
                    rs = statement.getGeneratedKeys();
                    if (!rs.next()) {
                        return false;
                    }
                    order_id = rs.getLong(1);

                    lines = (JSONArray) jsonObject.get("lines");
                    int len = lines.size();
                    for (int i = 0; i < len; i++) {
                        orderLine = (JSONObject) lines.get(i);
                        sql_insert = "INSERT INTO ORDERLINES (order_id, product_id, quantity, price, sum) VALUES (" + String.format("%d", order_id) + ", " + orderLine.get("product_id").toString() + ", " + orderLine.get("quantity").toString() + "," + orderLine.get("price").toString() + ", " + orderLine.get("sum").toString() + ");";
                        stmt.execute(sql_insert);
                    }
                }
                dbConnection.commit();
            } catch (Exception e) {
                if (dbConnection != null) {
                    try {
                        dbConnection.rollback();
                    } catch (SQLException ignore) {
                    }
                }
                log.log(Level.SEVERE, "Exception: ", e);
                return false;
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException ex) {
                    }
                }
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException ex) {
                    }
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException ignore) {
                    }
                }
            }
            return true;
        }

        Messenger getMessenger() {
            return messenger;
        }

        public boolean checkClient() throws SQLException, IOException, Exception {
            String deviceID = messenger.askDeviceID();
            if (deviceID == null) {
                return false;
            }
            if (!clientExist(deviceID)) {
                registerNewClient(deviceID);
            }
            mobileClient = getMobileClientByID(deviceID);
            return mobileClient != null;
        }

        public MobileClient getMobileClientByID(String deviceID) throws SQLException, Exception {
            MobileClient mc;
            Statement st = databaseManager.getDbConnection().createStatement();
            String deviceCheckQuery = "SELECT _id, device_id, name, CASE ISNULL(manager_id,0) WHEN 0 THEN 0 ELSE 1 END AS confirmed FROM DEVICES WHERE device_id = '" + deviceID + "'";
            ResultSet result = st.executeQuery(deviceCheckQuery);
            if (result.next()) {
                mc = new MobileClient(result.getLong("_id"), deviceID);
                mc.setName(result.getString("name"));
                mc.setConfirmed(result.getBoolean("confirmed"));
            } else {
                mc = null;
            }
            result.close();
            st.close();
            return mc;
        }

        public boolean clientExist(String deviceID) throws SQLException, Exception {
            Statement st = databaseManager.getDbConnection().createStatement();
            String deviceCheckQuery = "SELECT _id, device_id, name FROM DEVICES WHERE device_id = '" + deviceID + "'";
            ResultSet result = st.executeQuery(deviceCheckQuery);
            boolean returnResult = result.next();
            result.close();
            st.close();
            return returnResult;
        }

        public void registerNewClient(String deviceID) throws SQLException, Exception {
            initializeDevice(deviceID);
        }

        public void initializeDevice(String deviceID) throws SQLException, Exception {
            Statement st = databaseManager.getDbConnection().createStatement();
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
            for (String tableName : versionTables) {
                //nullTableVersionsQuery = "INSERT OR REPLACE INTO VERSIONS (device_id, table_name, version) VALUES ("+ String.format("%d", savedID) +", '"+ tableName +"', 0)";
                nullTableVersionsQuery = "IF NOT EXISTS (SELECT device_id from VERSIONS WHERE device_id = " + String.format("%d", savedID) + " AND table_name = '" + tableName + "')"
                        + "INSERT INTO VERSIONS (device_id, table_name, version) VALUES (" + String.format("%d", savedID) + ", '" + tableName + "', 0)"
                        + "ELSE "
                        + "UPDATE VERSIONS SET version = 0 WHERE device_id = " + String.format("%d", savedID) + " AND table_name = '" + tableName + "';";
                st.execute(nullTableVersionsQuery);
            }
            res.close();
            st.close();
        }

        public boolean clientIsConfirmed() {
            if (mobileClient.isConfirmed()) {
                messenger.sendDeviceConfirmation();
                return true;
            } else {
                messenger.sendDeviceRejection();
                return false;
            }
        }

        public void initClient() throws SQLException, Exception {
            initializeDevice(mobileClient.getDevice_id());
        }

        public void handleIntention() throws IOException, SQLException, ParseException, AddressException, Exception {
            boolean sendResult;
            switch (clientIntention) {
                case ExchangeProtocol.ClientIntention.INIT:
                    initClient();
                    //get updates for client and send them                        
                    sendResult = messenger.sendUpdates();
                    //if allright - update client versions
                    if (sendResult) {
                        updateClientVersions(mobileClient);
                    }
                    break;
                case ExchangeProtocol.ClientIntention.GET_UPDATES:
                    //get updates for client and send them                        
                    sendResult = messenger.sendUpdates();
                    //if allright - update client versions
                    if (sendResult) {
                        updateClientVersions(mobileClient);
                    }
                    break;
                case ExchangeProtocol.ClientIntention.SEND_ORDERS:
                    //recieve orders from client                          
                    boolean getOrdersResult = messenger.getOrders();
                    break;
                default:
                    //not sure what client want                    
                    break;
            }
        }

        public void updateClientVersions(MobileClient mobileClient) throws SQLException, Exception {
            HashMap updatedTableVersions = mobileClient.getUpdatedTableVersions();
            Statement st = null;
            for (Iterator it = updatedTableVersions.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                if ((Long) entry.getValue() > 0) {
                    String updateClientTableVersionsQuery = "UPDATE VERSIONS SET version = " + String.format("%d", entry.getValue()) + " WHERE table_name = '" + (String) entry.getKey() + "' and device_id = " + mobileClient.getId();
                    st = databaseManager.getDbConnection().createStatement();
                    st.executeUpdate(updateClientTableVersionsQuery);
                }
            }
            if (st != null) {
                st.close();
            }
        }

        public void sendNotificationsForOrders(ArrayList<String> ordersInJSONArrayList) throws ParseException, AddressException {
            for (String jsonString : ordersInJSONArrayList) {
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
                StringBuilder msgText = new StringBuilder();
                msgText.append("Поступил новый заказ из мобильного приложения");
                if (mobileClient.getName() != null && mobileClient.getName().length() > 0) {
                    msgText.append(" от ");
                    msgText.append(mobileClient.getName());
                }
                msgText.append('\n').append("Клиент: ").append(getClientNameByID((Long) jsonObject.get("client_id")));
                sendNotification(msgText.toString());
            }
        }

        public String getClientNameByID(long clientId) {
            String clientName;
            try {
                Statement st = databaseManager.getDbConnection().createStatement();
                String deviceCheckQuery = "SELECT name FROM CLIENTS WHERE _id = " + clientId;
                ResultSet result = st.executeQuery(deviceCheckQuery);
                if (result.next()) {
                    clientName = result.getString("name");
                } else {
                    clientName = null;
                }
                return clientName;
            } catch (Exception ex) {
                log.log(Level.SEVERE, "Exception: ", ex);
                return null;
            }
        }

        public void sendNotification(String msgText) throws AddressException {
            Properties p = getEmailProperties();
            p.put("recipient", "OL@rosel.ru, nikiforov.nikita@rosel.ru");
            p.put("subject", "Новый заказ из мобильного приложения");
            p.put("text", msgText);
            sendEmail(p);
        }

        public void checkIntention() throws IOException {

            switch (messenger.askIntention()) {
                case ExchangeProtocol.ClientIntention.SEND_ORDERS_STRING:
                    clientIntention = ExchangeProtocol.ClientIntention.SEND_ORDERS;
                    break;
                case ExchangeProtocol.ClientIntention.INIT_STRING:
                    clientIntention = ExchangeProtocol.ClientIntention.INIT;
                    break;
                case ExchangeProtocol.ClientIntention.GET_UPDATES_STRING:
                    clientIntention = ExchangeProtocol.ClientIntention.GET_UPDATES;
                    break;
            }

        }
    }

    public void initDB() {
        DbManager dbmanager = new DbManager(serverSettings);
        if (dbmanager.isConnected() && dbmanager.initializeDB()) {
            JOptionPane.showMessageDialog(null, "Database initialized!");
        } else {
            JOptionPane.showMessageDialog(null, "Can't initialize DB!");
        }
        dbmanager.disconnect();
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(SERVER_SOCKET);
            log.info("Server started");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.log(Level.INFO, "New connection {0}", clientSocket.toString());
                new ClientHandler(clientSocket).start();
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Exception: ", ex);
        }
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

    public static boolean checkEmailProperties(Properties prop) {
        if (!(prop.containsKey(ServerSettings.EMAIL_HOST)
                && prop.containsKey(ServerSettings.EMAIL_PORT)
                && prop.containsKey(ServerSettings.EMAIL_FROM)
                && prop.containsKey(ServerSettings.EMAIL_LOGIN)
                && prop.containsKey(ServerSettings.EMAIL_PASSWORD))) {
            return false;
        }
        try {
            Session session = Session.getDefaultInstance(prop, null);
            Transport transport = session.getTransport("smtp");
            transport.connect();
            return true;
        } catch (MessagingException ex) {
            log.log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public void sendEmail(Properties p) {
        Session session = Session.getDefaultInstance(p, null);
        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setFrom(p.getProperty("from"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(p.getProperty("recipient")));
            msg.setSubject(p.getProperty("subject"));
            msg.setSentDate(new Date());
            msg.setText(p.getProperty("text"));
            Transport.send(msg, p.getProperty("login"), p.getProperty("pwd"));
        } catch (MessagingException ex) {
            log.log(Level.SEVERE, "Exception: ", ex);
        }
    }

    public void saveEmailSettings(Properties settings) {
        if (checkEmailProperties(settings)) {
            saveSettings(settings);
        } else {
            JOptionPane.showMessageDialog(null, "Can't apply those settings");
        }
    }

    public void saveSettings(Properties settings) {
        serverSettings.putAll(settings);
        saveSettingsToFile(serverSettings);
    }

    public void saveSettingsToFile(Properties settings) {
        File settingsFile = new File("settings");
        try {
            if (!settingsFile.exists()) {
                settingsFile.createNewFile();
            }
            try (FileWriter fileWriter = new FileWriter(settingsFile)) {
                settings.store(fileWriter, null);
                fileWriter.flush();
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Exception: ", ex);
            return;
        }
        JOptionPane.showMessageDialog(null, "Settings saved");
    }

    public void setServerSettings(Properties serverSettings) {
        this.serverSettings = serverSettings;
    }

    public Properties getServerSettings() {
        return serverSettings;
    }

    public Properties loadSettings() {
        Properties settings = ServerSettings.getSettingsMap();
        File settingsFile = new File("settings");
        try {
            if (settingsFile.exists()) {
                try (FileReader fileReader = new FileReader(settingsFile)) {
                    settings.load(fileReader);
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Exception: ", ex);
        }
        return settings;
    }
}
