package modules.serverlogic;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class RoselServerApplication {
    
    private static final Logger LOG = Logger.getLogger(RoselServerApplication.class.getName());
    
    public static void main(String[] args) {
        initLogger();
        RoselServerApplication app = new RoselServerApplication();        
        app.startApp();        
    }
    
    void startApp(){
        RoselServerModel serverModel = new RoselServerModel();
        RoselServerController controller = new RoselServerController(serverModel);
    }
    
    public static void initLogger() {        
        try {
            LogManager.getLogManager().readConfiguration(RoselServerApplication.class.getResourceAsStream("logging.properties"));            
        } catch (IOException | SecurityException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
}
