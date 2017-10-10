package modules.serverlogic;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
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
import modules.data.RoselUpdateInfo;
import modules.transport.AcceptClientException;
import modules.transport.ServerTransport;
import modules.transport.TransportException;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class RoselServerModel {

    private SettingsManager settingsManager;            
    private ServerTransport transport;
    ConfigurableApplicationContext context;
    private RoselServerDAO roselServerDAO;
    private ArrayList<RoselServerModelObserverInterface> observers = new ArrayList<>(0);
    private static final Logger LOG = Logger.getLogger(RoselServerModel.class.getName());

    public void init() {
        context = new AnnotationConfigApplicationContext(RoselSpringConf.class);
        settingsManager = context.getBean(SettingsManager.class);
        try {
            settingsManager.loadSettings();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }        
    }

    public void applySettings(Properties settings) {
        if (SettingsManager.checkSettings(settings)) {
            settingsManager.setSettings(settings);
            saveServerSettings();
            if(roselServerDAO!=null){
                roselServerDAO.setDataSourceSettings(settingsManager.getSettings());
            }
        } else {
            notifyObservers("Can't save settings!");
        }
    }

    public void saveServerSettings() {
        try {
            settingsManager.saveSettings();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            notifyObservers("Can't save settings!");
        }
    }

    public Properties getServerSettings() {
        return settingsManager.getSettings();
    }

    public RoselServerDAO getDAO() {
        return roselServerDAO;
    }

    public void startServer() {

        try {
            roselServerDAO = context.getBean(RoselServerDAO.class);
            roselServerDAO.setDataSourceSettings(settingsManager.getSettings());            
            
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            notifyObservers("Can't connect to DB!");
            return;
        }

        if (transport == null) {
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

    public void stopServer() {
        if (context != null) {
            context.registerShutdownHook();
        }

        if (transport != null && transport.isStarted()) {
            transport.stop();
            notifyStateChanged();
        }
    }

    public void notifyStateChanged() {
        for (RoselServerModelObserverInterface obs : observers) {
            obs.onServerStateChange(transport.isStarted());
        }
    }

    public void notifyObservers(String msg) {
        for (RoselServerModelObserverInterface obs : observers) {
            obs.handleMsg(msg);
        }
    }

    public void registerRoselServerModelObserver(RoselServerModelObserverInterface obs) {
        observers.add(obs);
    }

    public void unregisterRoselServerModelObserver(RoselServerModelObserverInterface obs) {
        observers.remove(obs);
    }

    public synchronized void handleTransportException(Exception ex) {
        LOG.log(Level.SEVERE, "Transport Exception:", ex);
        stopServer();
    }

    public static ArrayList<String> getVersionTables() {
        ArrayList<String> tables = new ArrayList();
        tables.add("PRODUCTS");
        tables.add("CLIENTS");
        tables.add("PRICES");
        tables.add("ADDRESSES");
        return tables;
    }

    public RoselUpdateInfo getUpdateInfo(DeviceInfo deviceInfo, RoselUpdateInfo updateInfo){
        return roselServerDAO.getUpdateInfo(deviceInfo.getInnerId(), updateInfo);
    }

    public void postOrders(ArrayList<String> ordersInJSON, DeviceInfo deviceInfo){
        postOrdersInJSON(ordersInJSON, deviceInfo);
        if (ordersInJSON.size() > 0) {
            try {
                sendNotificationsForOrders(ordersInJSON, deviceInfo);
            } catch (MessagingException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            } catch (ParseException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    private void postOrdersInJSON(ArrayList<String> ordersInJSON, DeviceInfo deviceInfo) {
        roselServerDAO.postOrdersFromJSON(deviceInfo.getInnerId(), ordersInJSON);
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
        try {
            clientName = roselServerDAO.getClientName(clientId);
        } catch (Exception ex) {
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
        return settingsManager.getEmailProperties();
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

    public void handleConnectionException(Exception ex) {
        LOG.log(Level.SEVERE, "Transport Exception:", ex);
    }

    public void initializeDB() {

        if (transport != null && transport.isStarted()) {
            notifyObservers("Can't initialize! Stop server first!");
            return;
        }

        try {            
            if (roselServerDAO == null) {
                context = new AnnotationConfigApplicationContext(RoselSpringConf.class);
                roselServerDAO = context.getBean(RoselServerDAO.class);
            }
            roselServerDAO.setDataSourceSettings(settingsManager.getSettings());
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
