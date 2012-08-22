package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.events.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/21/12
 * Time: 2:37 PM
 *
 * Whenever we disconnect from the internet manually, don't reconnect automatically.
 */
public class StopReconnectStrategyObserver implements Observer<Boolean> {

    private SignalConnectionBase connection;

    public StopReconnectStrategyObserver(SignalConnectionBase connection) {
        this.connection = connection;
    }

    @Override
    public void notify(Object sender, Boolean networkDisconnect) {
        if (!networkDisconnect && connection.reconnectStrategy != null) {
            connection.reconnectStrategy.stop();
        }
    }

}
