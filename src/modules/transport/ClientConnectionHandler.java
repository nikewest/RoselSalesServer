package modules.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import modules.data.RoselUpdateInfo;
import modules.data.RoselUpdateItem;
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
        
        String clientIntention;
        String deviceId;
        
        try {
            //read intention
            clientIntention = reader.readLine();
            deviceId = reader.readLine();
            deviceInfo = server.getDeviceInfo(deviceId);
            if (!deviceInfo.isConfirmed()) {
                writer.println(TransportProtocol.NOT_REG);
                writer.flush();
                stopHandle();
                return;
            }
        } catch (IOException ex) {
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }
        
        switch (clientIntention) {
            case TransportProtocol.GET:                
                try {                    
                    writer.println(TransportProtocol.START_UPDATE);
                    writer.flush();
                    RoselUpdateInfo clientUpdateInfo;
                    RoselUpdateInfo updateInfo;
                    
                    String updateInfoJson = reader.readLine();
                    
                    while (true) {
                        //update information from client                        
                        if (updateInfoJson == null) {
                            stopHandle();
                            return;
                        }
                        clientUpdateInfo = RoselUpdateInfo.fromJSONString(updateInfoJson);
                        updateInfo  = server.getUpdateInfo(deviceInfo, clientUpdateInfo);
                        writer.println(updateInfo.toJSON());
                        writer.flush();
                        for(RoselUpdateItem updateItem:updateInfo.getUpdateItems()){
                            writer.println(updateItem.toString());                            
                        }
                        writer.flush();
                        updateInfoJson = reader.readLine();                        
                    }

                } catch (Exception ex) {
                    server.handleConnectionException(ex);
                    stopHandle();
                    return;
                }                
                
            case TransportProtocol.POST:
                try {                    
                    writer.println(TransportProtocol.START_POST);
                    writer.flush();
                    
                    ArrayList<String> ordersJson = new ArrayList<>();
                    
                    String orderJsonString;
                    
                    while ((orderJsonString = reader.readLine())!=null && !orderJsonString.equals(TransportProtocol.COMMIT)) {                        
                        ordersJson.add(orderJsonString);                                                                        
                    }
                    
                    server.postOrders(ordersJson, deviceInfo);
                    
                    writer.println(TransportProtocol.COMMIT);
                    writer.flush();
                    
                    stopHandle();                                     
                    
                    break;
                    
                } catch (Exception ex) {
                    server.handleConnectionException(ex);
                    stopHandle();
                    return;
                }  
        }
        
        stopHandle();        
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
