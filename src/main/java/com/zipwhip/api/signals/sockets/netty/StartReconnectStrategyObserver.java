package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.events.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/21/12
 * Time: 2:34 PM
 * <p/>
 * Whenever we reconnect, we start the strategy
 */
public class StartReconnectStrategyObserver implements Observer<ConnectionHandle> {

    private final SignalConnectionBase connectionBase;

    public StartReconnectStrategyObserver(SignalConnectionBase connection) {
        this.connectionBase = connection;
        if (this.connectionBase == null){
            throw new IllegalStateException("The connection cannot be null!");
        }

    }

    @Override
    public void notify(Object sender, ConnectionHandle connectionHandle) {
        // ignore the connected flag
        boolean connected = isConnected(connectionHandle);
        ReconnectStrategy strategy = getReconnectStrategy();

        if (connected && strategy != null) {
            strategy.start();
        }
    }

    protected ReconnectStrategy getReconnectStrategy() {
        return this.connectionBase.getReconnectStrategy();
    }

    protected boolean isConnected(ConnectionHandle connectionHandle) {
        boolean connected = false;
        if (connectionHandle != null) {
            // connected is elusive. Best we can do is check destruction state.
            connected = !connectionHandle.isDestroyed();
        }
        return connected;
    }

}
