package roselsalesserver;

public final class ExchangeProtocol {

    //response strings
    public static final String OK_RESPONSE = "OK";
    public static final String DEVICE_ID_REQUEST = "DEVICE_ID_REQUEST"; 
    public static final String INTENTION_REQUEST = "INTENTION_REQUEST";
    public static final String CONFIRMATION_REQUEST = "CONFIRMATION_REQUEST";
    public static final String DEVICE_CONFIRMATION = "DEVICE_CONFIRMATION";
    public static final String DEVICE_REJECTION = "DEVICE_REJECTION";
    
    public abstract class ClientIntention {

        //intention strings
        public static final String SEND_ORDERS_STRING = "ORDERS";
        public static final String GET_UPDATES_STRING = "UPDATE";
        public static final String INIT_STRING = "INIT";

        //intention codes
        public static final int SEND_ORDERS = 2;
        public static final int GET_UPDATES = 1;
        public static final int INIT = 0;        
        
    }
}
