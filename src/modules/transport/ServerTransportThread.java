package modules.transport;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        while(true){
            try {
                Socket clientSocket = transport.acceptClient();
                new ClientConnectionHandler(clientSocket, transport.getTransportListener()).start();
            } catch (IOException ex) {
                transport.handleException(ex);
            }
        }
    }
    
}
