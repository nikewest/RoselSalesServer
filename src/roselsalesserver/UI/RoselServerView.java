package roselsalesserver.UI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import modules.serverlogic.RoselServerController;
import modules.serverlogic.RoselServerModel;
import modules.serverlogic.RoselServerModelObserverInterface;

/**
 *
 * @author nikiforovnikita
 */
public class RoselServerView implements RoselServerModelObserverInterface{
    
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
        
        mainPanel.loadSettingsToUI(server.getServerSettings());
        mainPanel.setServerStateLabel(false);
        server.registerRoselServerModelObserver(this);
        
        mainFrame.pack();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);
    }
    
    public void initButtonActionPerformed(){
        serverController.initDB();
    }
    
    public void startServer(){
        serverController.startServer();
    }
    
    public void stopServer(){
        serverController.stopServer();
    }
    
    public void saveSettings(Properties settings){
        serverController.saveSettings(settings);
    }

    @Override
    public void handleMsg(String msg) {
        JOptionPane.showMessageDialog(null, msg);
    }

    @Override
    public void onServerStateChange(boolean state) {
        mainPanel.setServerStateLabel(state);
    }
    
}
