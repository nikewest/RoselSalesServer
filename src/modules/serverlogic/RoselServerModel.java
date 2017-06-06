package modules.serverlogic;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import modules.transport.AcceptClientException;
import modules.transport.ServerTransport;
import modules.transport.ServerTransportListener;
import modules.transport.StartServerException;

/**
 *
 * @author nikiforovnikita
 */
public class RoselServerModel implements ServerTransportListener{
    
    private Properties serverSettings;    
    private ServerTransport transport;
    private ArrayList<RoselServerModelObserver> observers = new ArrayList<>(0);
    private static final Logger LOG = Logger.getLogger(RoselServerModel.class.getName());
    
    public void init(){  
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
    
    // TRANSPORT
    public void startServer(){
        
        if(transport==null){
            transport = new ServerTransport();
        }        
        try {
            transport.start();
            LOG.info("Server started");
            notifyStateChanged();
            //transport.waitForConnections();
        } catch (StartServerException | AcceptClientException ex) {
            LOG.log(Level.SEVERE, null, ex);
            notifyObservers("Can't start server!");
            return;
        }
    }
    
    public void stopServer(){
        transport.stop();
        LOG.info("Server stoped");
        notifyStateChanged();
    }
    
    public void notifyStateChanged(){
        for(RoselServerModelObserver obs : observers){
            obs.onServerStateChange(transport.isStarted());
        }
    }
    
    public void notifyObservers(String msg){
        for(RoselServerModelObserver obs : observers){
            obs.handleMsg(msg);
        }
    }
    
    public void registerRoselServerModelObserver(RoselServerModelObserver obs){
        observers.add(obs);
    }
    
    public void unregisterRoselServerModelObserver(RoselServerModelObserver obs){
        observers.remove(obs);
    }

    @Override
    public void handleTransportException(Exception ex) {
        if(ex instanceof AcceptClientException){
            notifyStateChanged();
        }
    }
}
