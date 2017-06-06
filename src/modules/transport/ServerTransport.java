package modules.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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

    public ServerTransport() {
        serverSocketPort = 60611;
    }
    
    public void start() throws StartServerException, AcceptClientException {
        try {
            serverSocket = new ServerSocket(getServerSocketPort());            
            setStarted(true);                        
            transportThread = new ServerTransportThread(this);
            transportThread.start();
        } catch (IOException ex) {
            throw new StartServerException(ex);            
        }
    }
    
    public void stop() {
        setStarted(false);
        transportThread.interrupt();
        try {
            getServerSocket().close();
        } catch (IOException ignore) {}
    }
    
    public void handleException(Exception ex){
        transportListener.handleTransportException(ex);        
    }

    /**
     * @return the serverSocketPort
     */
    public int getServerSocketPort() {
        return serverSocketPort;
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
     * @return the serverSocket
     */
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    /**
     * @param transportListener the transportListener to set
     */
    public void setTransportListener(ServerTransportListener transportListener) {
        this.transportListener = transportListener;
    }
    
}
