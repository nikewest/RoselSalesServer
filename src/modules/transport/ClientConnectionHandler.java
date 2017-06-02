package modules.transport;

import java.net.Socket;

/**
 *
 * @author nikiforovnikita
 */
public class ClientConnectionHandler extends Thread {

    private Socket clientSocket;

    public ClientConnectionHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }
    
    @Override
    public void run() {
        
    }
    
}
