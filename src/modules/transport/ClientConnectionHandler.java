package modules.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import modules.data.RoselJsonParser;
import modules.data.RoselUpdateInfo;
import modules.data.RoselUpdateItem;
import modules.data.RoselUpdateMap;
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
        try {
            //read intention
            clientIntention = reader.readLine();            
        } catch (IOException ex) {
            server.handleConnectionException(ex);
            stopHandle();
            return;
        }   
        
        switch (clientIntention) {
            case TransportMessage.GET:
                String deviceId;
                try {
                    deviceId = reader.readLine();
                    deviceInfo = server.getDeviceInfo(deviceId);
                    if (!deviceInfo.isConfirmed()) {
                        writer.println(TransportMessage.NOT_REG);
                        writer.flush();
                        stopHandle();
                        return;
                    }
                    writer.println(TransportMessage.START_UPDATE);
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
                        clientUpdateInfo = RoselJsonParser.fromJSONString(updateInfoJson);
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

            case TransportMessage.POST:
                break;
        }
        
        stopHandle();        
    }
    
//    public void handleClientRequest(TransportMessage request, DeviceInfo deviceInfo){
//        
//        TransportMessage response = new TransportMessage();
//        response.setDevice_id(TransportMessage.SERVER_ID);
//        if (!deviceInfo.isConfirmed()) {
//            response.setIntention(TransportMessage.NOT_REG);
//            response.setEmptyBody();
//            write(response.toJSONString());
//        }
//
//        //check intention
//        switch (request.getIntention()) {
//            case TransportMessage.GET: // TYPE "GET" - request for data updates 
//                response.setIntention(TransportMessage.UPDATE);
//                RoselUpdateMap updateMap = server.getUpdates(deviceInfo, request.getBody());                
//                response.getBody().add(updateMap.getJsonUpdateInfo());
//                write(response.toJSONString());
//                write((String[]) updateMap.getUpdateBody().toArray());
//                break;
//            case TransportMessage.POST: // TYPE "POST" - request with orders
//                server.postOrders(request.getBody(), deviceInfo);
//                response.setIntention(TransportMessage.POST_COMMIT);
//                write(response.toJSONString());
//                break;
//            default: //if wrong type?
//                break;
//        }
//    }
    
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
