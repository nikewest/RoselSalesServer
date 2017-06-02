package modules.transport;

/**
 *
 * @author nikiforovnikita
 */
public class StartServerException extends Exception {

    public StartServerException(Exception ex) {
        super("Start server socket failure", ex);
    }
    
}
