package modules.transport;

/**
 *
 * @author nikiforovnikita
 */
public class AcceptClientException extends Exception {
    
    public AcceptClientException(Exception ex) {
        super("Accept new client connection failure", ex);
    }
    
}
