package modules.transport;

import java.io.Serializable;
import java.util.ArrayList;

public class TransportMessage implements Serializable{
    
    public static final String END = "@";
    
    //ids
    public static final String SERVER_ID = "ROSEL.SERVER";
    
    //intentions TYPES
    public static final String NOT_REG = "ROSEL.NOT_REG"; //device not registered on server
    public static final String UPDATE = "ROSEL.UPDATE"; //updates sending
    public static final String POST_COMMIT = "ROSEL.POST_COMMIT"; //commit post on server
    
    public static final String GET = "ROSEL.GET"; // intention to get updates
    public static final String POST = "ROSEL.POST"; //intention to post orders
    
    private String device_id;
    private String intention;
    private ArrayList<String> body = new ArrayList<>();    
    
    public static TransportMessage fromString(String msg) throws TransportMessageException{
        TransportMessage transportMsg = new TransportMessage();
        String[] temp = msg.split("\n");
        if(temp.length < 2) {
            throw new TransportMessageException();
        }
        transportMsg.setDevice_id(temp[0]);
        transportMsg.setIntention(temp[1]);
        transportMsg.setBody(new ArrayList<>(0));
        for(int i=2;i<temp.length;i++){
            transportMsg.getBody().add(temp[i]);
        }
        return transportMsg;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(device_id).append("\n");
        str.append(intention).append("\n");
        for(String curString:body){
            str.append(curString).append("\n");
        }
        str.append(TransportMessage.END);
        return str.toString();
    }    
    
    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getIntention() {
        return intention;
    }

    public void setIntention(String intention) {
        this.intention = intention;
    }

    public ArrayList<String> getBody() {
        return body;
    }

    public void setBody(ArrayList<String> body) {
        this.body = body;
    }

    public void setEmptyBody(){
        this.body = new ArrayList<>(0);
    }
}
