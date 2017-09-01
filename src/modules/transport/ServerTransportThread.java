package modules.transport;

import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author nikiforovnikita
 */
public class ServerTransportThread extends Thread {

    ServerTransport transport;
    
    public ServerTransportThread(ServerTransport transport) {
        this.transport = transport;
    }
    
    @Override
    public void run() {
        waitForConnections();
    }
    
    public void stopTransportThread(){        
        this.interrupt();
    }
    
    public void waitForConnections() {
        while (transport.isStarted()) {            
            try {
                Socket clientSocket = transport.acceptClient();
                new ClientConnectionHandler(clientSocket, transport.getRoselServer()).start();
            } catch (IOException ex) {                
                stopTransportThread();                
                transport.handleException(ex);                                
            }
        }
    }
    
}
