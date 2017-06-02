package roselsalesserver.UI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import modules.serverlogic.RoselServerController;
import modules.serverlogic.RoselServerModel;
import modules.serverlogic.RoselServerModelObserver;

/**
 *
 * @author nikiforovnikita
 */
public class RoselServerView implements RoselServerModelObserver{
    
    private final RoselServerController serverController;
    private final RoselServerModel server;
    private RoselServerPanel mainPanel;

    public RoselServerView(RoselServerController controller, RoselServerModel model) {
        this.serverController = controller;
        this.server = model;        
    }
    
    public void buildUI(){        
        JFrame mainFrame = new JFrame("Rosel server");
        mainPanel = new RoselServerPanel(this);
        mainFrame.getContentPane().add(mainPanel);
        mainFrame.pack();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);
        
        mainPanel.loadSettingsToUI(server.getServerSettings());
        server.registerRoselServerModelObserver(this);
    }
    
    public void initButtonActionPerformed(){
        serverController.initDB();
    }
    
    public void saveDatabaseSettings(Properties settings){
        
    }
    
    public void saveEmailSettings(Properties settings){
        
    }

    @Override
    public void handleErrorMsg(String msg) {
        JOptionPane.showMessageDialog(null, msg);
    }
    
}
