package roselsalesserver;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ServerApplication {
    
    private static Logger log = Logger.getLogger(ServerApplication.class.getName());
    
    public static void main(String[] args) {
        initLogger();
        ServerApplication app = new ServerApplication();
        app.startApplication();        
    }
    
    public void startApplication(){        
        RoselSalesServer server = new RoselSalesServer();
        server.setServerSettings(server.loadSettings());
        server.buildUI();         
        server.startServer();
        
    };
    
    public static void initLogger() {
        
        try {
            LogManager.getLogManager().readConfiguration(ServerApplication.class.getResourceAsStream("logging.properties"));            
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }
}
