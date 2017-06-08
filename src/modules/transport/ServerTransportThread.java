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
        try {
            waitForConnections();
        } catch (AcceptClientException ex) {
            transport.handleException(ex);
        }
    }
    
    public void stopTransportThread(){
        this.interrupt();
    }
    
    public void waitForConnections() throws AcceptClientException{        
        while(transport.isStarted()){
            try {
                Socket clientSocket = transport.getServerSocket().accept();
                new ClientConnectionHandler(clientSocket, transport.getTransportListener()).start();
            } catch (IOException ex) {
                if(!isInterrupted()) throw new AcceptClientException(ex);
            }
        }
    }
    
}
