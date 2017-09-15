package modules.serverlogic;

public interface RoselServerModelObserverInterface {
    
    public void handleMsg(String msg);
    public void onServerStateChange(boolean state);    
    
}
