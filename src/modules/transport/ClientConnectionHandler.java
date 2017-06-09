package modules.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import modules.serverlogic.ClientModel;

/**
 *
 * @author nikiforovnikita
 */

public class ClientConnectionHandler extends Thread {

    private final Socket clientSocket;
    private PrintWriter writer;    
    private BufferedReader reader;    
    private final ServerTransportListener server;
    private ClientModel clientModel;
    
    public ClientConnectionHandler(Socket clientSocket, ServerTransportListener server) {
        this.clientSocket = clientSocket;  
        this.server = server;
    }
    
    @Override
    public void run() {        
        try {
            writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException ex) {                        
            server.handleTransportException(ex);
            stopHandle();
            return;
        }
        try{
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException ex) {                        
            server.handleTransportException(ex);
            stopHandle();
            return;
        }        
        String clientsRequest = read();
        TransportMessage request;
        try {
            request = TransportMessage.fromString(clientsRequest);
        } catch (TransportMessageException ex) {
            server.handleTransportException(ex);
            stopHandle();
            return;
        }
        try {
            clientModel = server.buildClientModel(request);
        } catch (Exception ex) {
            server.handleTransportException(ex);
            stopHandle();
            return;
        }
        TransportMessage response;
        try {
            response = server.handleClientRequest(request, clientModel);
        } catch (Exception ex) {
            server.handleTransportException(ex);
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
        //this.interrupt();
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
            while((line = reader.readLine()) != null){
                stringBuilder.append(line);
            }
        } catch (IOException ex) {            
        }
        return stringBuilder.toString();
    }
    
    public void write(String... stringData){
        for(String curString:stringData){
            writer.println(curString);
            writer.flush();
        }
    }
    
}
