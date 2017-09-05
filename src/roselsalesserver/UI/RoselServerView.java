package roselsalesserver.UI;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import modules.serverlogic.RoselServerController;
import modules.serverlogic.RoselServerModel;
import modules.serverlogic.RoselServerModelObserverInterface;

public class RoselServerView implements RoselServerModelObserverInterface, WindowListener{
    
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
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(this);
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

    @Override
    public void windowOpened(WindowEvent e) {        
    }

    @Override
    public void windowClosing(WindowEvent e) {
        int onExitUserConfirm = JOptionPane.showConfirmDialog(mainPanel, "Are you sure to exit?", "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(onExitUserConfirm == JOptionPane.OK_OPTION){
            stopServer();
            System.exit(0);
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {        
    }

    @Override
    public void windowIconified(WindowEvent e) {        
    }

    @Override
    public void windowDeiconified(WindowEvent e) {        
    }

    @Override
    public void windowActivated(WindowEvent e) {        
    }

    @Override
    public void windowDeactivated(WindowEvent e) {        
    }
    
}
