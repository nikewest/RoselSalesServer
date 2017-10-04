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
            //request = TransportMessage.fromString(clientsRequest);
            request = TransportMessage.fromJSONString(clientsRequest);
        } catch (Exception ex) {            
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
       
        write(response.toJSONString());
        
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

    public String read() {
        try {
            String line = reader.readLine();
            if (line != null) {
                return line;
            }
        } catch (IOException ex) {
            server.handleConnectionException(ex);
        }
        return "";
    }
    
    public void write(String... stringData){
        for(String curString:stringData){
            writer.println(curString);            
        }
        writer.flush();
    }
    
}
