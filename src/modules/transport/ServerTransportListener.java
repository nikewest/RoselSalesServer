package modules.transport;

import modules.serverlogic.ClientModel;

/**
 *
 * @author nikiforovnikita
 */
public interface ServerTransportListener {
    public void handleTransportException(Exception ex);        
    public void handleConnectionException(Exception ex);        
    public ClientModel buildClientModel(TransportMessage request) throws Exception;
    public TransportMessage handleClientRequest(TransportMessage request, ClientModel clientModel) throws Exception;
    public void commitClientsUpdate(ClientModel clientModel);
}