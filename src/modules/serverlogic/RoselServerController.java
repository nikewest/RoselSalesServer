package modules.serverlogic;

import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.JPanel;
import roselsalesserver.UI.RoselServerPanel;
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
    
    public void saveDatabaseSettings(Properties settings){
        
    }
    
    public void saveEmailSettings(Properties settings){
        
    }
    
    public Properties getServerSettings(){
        return new Properties();
    }
    
}
