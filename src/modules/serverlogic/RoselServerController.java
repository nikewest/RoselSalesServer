package modules.serverlogic;

import java.util.Properties;
import roselsalesserver.UI.RoselServerView;

/**
 *
 * @author nikiforovnikita
 */
public class RoselServerController {    
    
    RoselServerModel roselServerModel;
    RoselServerView roselServerView;    
    
    public RoselServerController(RoselServerModel roselServerModel) {        
        this.roselServerModel = roselServerModel;                
        roselServerModel.init();       
        
        roselServerView = new RoselServerView(this, roselServerModel);
        roselServerView.buildUI();
    }
    
    public void initDB(){
        
    }
    
    public void startServer(){
        roselServerModel.startServer();
    }
    
    public void stopServer(){
        roselServerModel.stopServer();
    }
    
    public void saveSettings(Properties settings){
        roselServerModel.applySettings(settings);
    }
    
    public Properties getServerSettings(){
        return new Properties();
    }
    
}
