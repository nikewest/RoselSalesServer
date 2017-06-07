package modules.transport;

import java.util.ArrayList;

/**
 *
 * @author nikiforovnikita
 */
public interface ClientsRequestHandlerInterface {
    
    public void handleException(Exception ex);
    public TransportMessage handleRequest(TransportMessage request) throws Exception;    
    
}
