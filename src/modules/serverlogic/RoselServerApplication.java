package modules.serverlogic;

import roselsalesserver.UI.RoselServerPanel;

/**
 *
 * @author nikiforovnikita
 */
public class RoselServerApplication {
    
    public static void main(String[] args) {
        RoselServerApplication app = new RoselServerApplication();
        app.startApp();
    }
    
    void startApp(){
        RoselServerModel serverModel = new RoselServerModel();
        RoselServerController controller = new RoselServerController(serverModel);
    }
}
