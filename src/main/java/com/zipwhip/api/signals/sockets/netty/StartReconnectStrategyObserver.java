package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.sockets.netty.SignalConnectionBase;
import com.zipwhip.events.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/21/12
 * Time: 2:34 PM
 *
 * Whenever we reconnect, we start the strategy
 */
public class StartReconnectStrategyObserver implements Observer<Boolean> {

    private SignalConnectionBase connection;

    public StartReconnectStrategyObserver(SignalConnectionBase connection) {
        this.connection = connection;
    }

    @Override
    public void notify(Object sender, Boolean connected) {
        // ignore the connected flag
        if (connection.isConnected()) {
            if (connection.reconnectStrategy != null){
                connection.reconnectStrategy.start();
            }
        }
    }
}
