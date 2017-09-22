package modules.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import modules.serverlogic.DeviceInfo;
import modules.serverlogic.RoselServerModel;

public class ClientConnectionHandler extends Thread {

    private final Socket clientSocket;
    private PrintWriter writer;    
    private BufferedReader reader;    
    private final RoselServerModel server;
    private DeviceInfo deviceInfo;
    
    public ClientConnectionHandler(Socket clientSocket, RoselServerModel server) {
        this.clientSocket = clientSocket;  
        this.server = server;        
    }
    
    @Override
    public void run() {        
        try {
            writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException ex) {                                    
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }
        try{
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException ex) {                                    
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }        
        String clientsRequest = read();
        TransportMessage request;
        try {
            request = TransportMessage.fromString(clientsRequest);
        } catch (TransportMessageException ex) {            
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }
        try {
            deviceInfo = server.getDeviceInfo(request.getDevice_id());
        } catch (Exception ex) {            
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }
        TransportMessage response;
        try {
            response = server.handleClientRequest(request, deviceInfo);
        } catch (Exception ex) {            
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }
        write(response.toString());
        if (response.getIntention().equals(TransportMessage.UPDATE)) {
            //server.commitClientsUpdate(clientModel);
        }
        freeRes();        
    }
    
    public void stopHandle(){
        freeRes();        
    }
    
    public void freeRes(){
        deviceInfo = null;
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
