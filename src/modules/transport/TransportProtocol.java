package modules.transport;

public abstract class TransportProtocol {

    public static final String END = "@";

    //ids
    public static final String SERVER_ID = "ROSEL.SERVER";

    //intentions TYPES
    public static final String NOT_REG = "ROSEL.NOT_REG"; //device not registered on server
    public static final String START_UPDATE = "ROSEL.START_UPDATE"; 
    public static final String START_POST = "ROSEL.START_POST";     
    public static final String COMMIT = "ROSEL.COMMIT";

    public static final String GET = "ROSEL.GET"; // intention to get updates
    public static final String POST = "ROSEL.POST"; //intention to post orders
  
}
