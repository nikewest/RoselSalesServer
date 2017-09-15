package modules.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;
import modules.serverlogic.RoselServerModel;

public class ServerTransport {
    
    private ServerSocket serverSocket;
    private int serverSocketPort;
    private boolean started = false;    
    private ServerTransportThread transportThread;    
    private RoselServerModel roselServer;
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
    
    public synchronized void handleException(Exception ex){
        if(ex instanceof SocketException && !isStarted()){
            return;
        }
        getRoselServer().handleTransportException(ex);        
    }

    public Socket acceptClient() throws IOException{
        return serverSocket.accept();
    }        

    public void setServerSocketPort(int serverSocketPort) {
        this.serverSocketPort = serverSocketPort;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public RoselServerModel getRoselServer() {
        return roselServer;
    }

    public void setRoselServer(RoselServerModel roselServer) {
        this.roselServer = roselServer;
    }

   
    
}
