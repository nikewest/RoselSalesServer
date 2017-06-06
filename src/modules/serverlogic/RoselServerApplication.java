package modules.serverlogic;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import roselsalesserver.ServerApplication;

/**
 *
 * @author nikiforovnikita
 */
public class RoselServerApplication {
    
    private static Logger LOG = Logger.getLogger(ServerApplication.class.getName());
    
    public static void main(String[] args) {
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
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
}
