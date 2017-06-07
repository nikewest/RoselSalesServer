package modules.transport;

/**
 *
 * @author nikiforovnikita
 */
public interface ServerTransportListener {
    public void handleTransportException(Exception ex);
    public ClientsRequestHandlerInterface getRequestHandler();
}
