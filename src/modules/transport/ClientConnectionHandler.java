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

/**
 *
 * @author nikiforovnikita
 */

public class ClientConnectionHandler extends Thread {

    private final Socket clientSocket;
    private PrintWriter writer;    
    private BufferedReader reader;    
    private ClientsRequestHandlerInterface requestHandler;
    
    public ClientConnectionHandler(Socket clientSocket, ClientsRequestHandlerInterface requestHandler) {
        this.clientSocket = clientSocket;  
        this.requestHandler = requestHandler;
    }
    
    @Override
    public void run() {        
        try {
            writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException ex) {            
            requestHandler.handleException(ex);            
            stopHandle();
        }        
        
        String clientsRequest = read();
        try {            
            TransportMessage request = TransportMessage.fromString(clientsRequest);            
            TransportMessage response = requestHandler.handleRequest(request);
            write(response.toString());            
        } catch (TransportMessageException ex) {            
            requestHandler.handleException(ex);
            stopHandle();
        } catch (Exception ex) {        
            requestHandler.handleException(ex);
            stopHandle();
        }
        
        freeRes();
    }
    
    public void stopHandle(){
        freeRes();
        this.interrupt();
    }
    
    public void freeRes(){
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
            requestHandler.handleException(ex);            
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
