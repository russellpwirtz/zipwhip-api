package com.zipwhip.api.signals;

import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.signals.presence.Presence;

/**
 * Date: 7/30/13
 * Time: 6:09 PM
 *
 * Executes /signals/connect against Zipwhip to bind a clientId + sessionKey.
 *
 * @author Michael
 * @version 1
 */
public interface SignalsConnectActor {

    ObservableFuture<Void> connect(String clientId, String sessionKey, String subscriptionId, Presence presence);

    ObservableFuture<Void> disconnect(String clientId, String sessionKey, String subscriptionId);

}
