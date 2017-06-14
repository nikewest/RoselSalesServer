package modules.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

/**
 *
 * @author nikiforovnikita
 */
public class ServerTransport {
    
    private ServerSocket serverSocket;
    private int serverSocketPort;
    private boolean started = false;
    private ServerTransportThread transportThread;
    private ServerTransportListener transportListener;
    private static final Logger LOG = Logger.getLogger(ServerTransport.class.getName());
    
    public ServerTransport() {
        serverSocketPort = 60611;
    }
    
    public void start() throws TransportException, AcceptClientException {
        if (isStarted()) {
            return;
        }
        try {
            serverSocket = new ServerSocket(serverSocketPort);
        } catch (IOException ex) {
            throw new TransportException(ex);
        }
        setStarted(true);
        transportThread = new ServerTransportThread(this);
        transportThread.start();
        LOG.info("Server started");                  
    }
    
    public void stop() {
        if(!isStarted()){
            return;
        }        
        if(transportThread!=null && transportThread.isAlive()){                                
            transportThread.stopTransportThread();            
        }
        setStarted(false);        
        LOG.info("Server stoped");
        try {
            serverSocket.close();
        } catch (IOException ignore) {}        
    }
    
    public void handleException(Exception ex){
        transportListener.handleTransportException(ex);        
    }

    public Socket acceptClient() throws IOException{
        return serverSocket.accept();
    }        

    /**
     * @param serverSocketPort the serverSocketPort to set
     */
    public void setServerSocketPort(int serverSocketPort) {
        this.serverSocketPort = serverSocketPort;
    }

    /**
     * @return the started
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * @param started the started to set
     */
    public void setStarted(boolean started) {
        this.started = started;
    }

    /**
     * @param transportListener the transportListener to set
     */
    public void setTransportListener(ServerTransportListener transportListener) {
        this.transportListener = transportListener;
    }

    /**
     * @return the transportListener
     */
    public ServerTransportListener getTransportListener() {
        return transportListener;
    }
    
}
