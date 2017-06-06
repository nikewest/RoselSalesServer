package modules.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author nikiforovnikita
 */
public class ClientConnectionHandler extends Thread {

    private final Socket clientSocket;
    private PrintWriter writer;    
    private BufferedReader reader;    
    
    public ClientConnectionHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;        
    }
    
    @Override
    public void run() {
        
        try {
            writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            //handle client connection exception
        }        
        
        //check client
            // - new => write to db tables
            // - not confirmed => close connection
            // - confirmed => resume
            
            //check intention
            // - get updates => send updates
            // - send orders => recieve orders
        
        writer.close();
        try {
            reader.close();
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
            //handle exception
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
