package modules.transport;

/**
 *
 * @author nikiforovnikita
 */
public class TransportException extends Exception {

    public TransportException(Exception ex) {
        super("Start server socket failure", ex);
    }
    
}
