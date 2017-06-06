package modules.serverlogic;

/**
 *
 * @author nikiforovnikita
 */
public interface RoselServerModelObserver {
    
    public void handleMsg(String msg);
    public void onServerStateChange(boolean state);    
    
}
