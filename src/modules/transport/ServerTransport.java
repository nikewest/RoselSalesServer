package modules.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nikiforovnikita
 */
public class ServerTransport {
    
    private ServerSocket serverSocket;
    private int serverSocketPort;
    private boolean started = false;
    
    void start() throws StartServerException {
        try {
            serverSocket = new ServerSocket(getServerSocketPort());            
            setStarted(true);                        
        } catch (IOException ex) {
            throw new StartServerException(ex);
        }
    }
    
    void waitForConnections() throws AcceptClientException{
        while(isStarted()){
            try {
                Socket clientSocket = getServerSocket().accept();
                new ClientConnectionHandler(clientSocket).start();
            } catch (IOException ex) {
                throw new AcceptClientException(ex);
            }
        }
    }
    
    void stop() {
        setStarted(false);
        try {
            getServerSocket().close();
        } catch (IOException ignore) {}
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
    private boolean isStarted() {
        return started;
    }

    /**
     * @param started the started to set
     */
    private void setStarted(boolean started) {
        this.started = started;
    }

    /**
     * @return the serverSocket
     */
    private ServerSocket getServerSocket() {
        return serverSocket;
    }
    
}
