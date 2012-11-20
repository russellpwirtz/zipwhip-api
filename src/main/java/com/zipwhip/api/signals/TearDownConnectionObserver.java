package com.zipwhip.api.signals;

import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionHandleAware;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Created with IntelliJ IDEA.
* User: Michael
* Date: 9/10/12
* Time: 6:20 PM
* To change this template use File | Settings | File Templates.
*/
public class TearDownConnectionObserver<T> implements Observer<ObservableFuture<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TearDownConnectionObserver.class);

    private final boolean reconnect;

    public TearDownConnectionObserver(boolean reconnect) {
        this.reconnect = reconnect;
    }

    @Override
    public void notify(Object sender, ObservableFuture<T> signalsConnectFuture) {
        ConnectionHandleAware task = (ConnectionHandleAware) sender;
        ConnectionHandle connectionHandle = task.getConnectionHandle();

        if (reconnect) {
            LOGGER.error("SignalsConnectFuture was not successful. (failed to receive a SubscriptionCompleteCommand?) " +
                    "We're going to reconnect.");
        } else {
            LOGGER.error("SignalsConnectFuture was not successful. (failed to receive a SubscriptionCompleteCommand?) " +
                    "We're going to disconnect.");
        }

        // we are in the Timer thread (pub sub if Timer is Intent based).
        // hashwheel otherwise.

        // we've decided to clear the clientId when the signals/connect doesn't work
        // this connection object lets us be certain that the current connection is reconnected.

        if (connectionHandle == null) {
            LOGGER.error("Cannot tearDown connection because the connectionHandle was null! (Maybe the SignalConnection crashed during its phase?) We should be already disconnected?");
            return;
        }

        synchronized (connectionHandle) {
            if (connectionHandle.isDestroyed()) {
                LOGGER.error(String.format("The connectionHandle we started with (%s) has been destroyed. We are stale! Quitting", connectionHandle));
                return;
            }

            // This should be sufficient to start the whole cycle over again.

            // NOTE we can be sure that it's the right "connection" that we're killing since
            // our connectionHandle was created special for this request.
            if (reconnect) {
                LOGGER.error("Called connectionHandle.reconnect()");
                connectionHandle.reconnect();
            } else {
                LOGGER.error("Called connectionHandle.disconnect(false)");
                connectionHandle.disconnect();
            }
        }
    }

    @Override
    public String toString() {
        return "TearDownConnectionObserver";
    }
}
