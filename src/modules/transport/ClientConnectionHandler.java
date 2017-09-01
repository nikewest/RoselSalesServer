package modules.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import modules.serverlogic.ClientModel;
import modules.serverlogic.RoselServerModel;

/**
 *
 * @author nikiforovnikita
 */

public class ClientConnectionHandler extends Thread {

    private final Socket clientSocket;
    private PrintWriter writer;    
    private BufferedReader reader;    
    private final RoselServerModel server;
    private ClientModel clientModel;
    
    public ClientConnectionHandler(Socket clientSocket, RoselServerModel server) {
        this.clientSocket = clientSocket;  
        this.server = server;
    }
    
    @Override
    public void run() {        
        try {
            writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException ex) {                        
            //server.handleTransportException(ex);
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }
        try{
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException ex) {                        
            //server.handleTransportException(ex);
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }        
        String clientsRequest = read();
        TransportMessage request;
        try {
            request = TransportMessage.fromString(clientsRequest);
        } catch (TransportMessageException ex) {
            //server.handleTransportException(ex);
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }
        try {
            clientModel = server.buildClientModel(request);
        } catch (Exception ex) {
            //server.handleTransportException(ex);
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }
        TransportMessage response;
        try {
            response = server.handleClientRequest(request, clientModel);
        } catch (Exception ex) {
            //server.handleTransportException(ex);
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }
        write(response.toString());
        if (response.getIntention().equals(TransportMessage.UPDATE)) {
            server.commitClientsUpdate(clientModel);
        }
        freeRes();        
    }
    
    public void stopHandle(){
        freeRes();        
    }
    
    public void freeRes(){
        clientModel = null;
        writer.close();
        try {
            reader.close();
        } catch (IOException ignore) {            
        }                
        try {
            clientSocket.close();
        } catch (IOException ignore) {            
        }        
    }
    
    public String read(){
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            while((line = reader.readLine())!= null && !line.equals(TransportMessage.END)){
                stringBuilder.append(line).append('\n');
            }
        } catch (IOException ex) {            
        }
        return stringBuilder.toString();
    }
    
    public void write(String... stringData){
        for(String curString:stringData){
            writer.println(curString);            
        }
        writer.flush();
    }
    
}
