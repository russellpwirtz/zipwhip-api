package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.util.Asserts;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/21/12
 * Time: 2:37 PM
 *
 * Whenever we disconnect from the internet manually, don't reconnect automatically.
 */
public class StopReconnectStrategyObserver extends StartReconnectStrategyObserver {

    public StopReconnectStrategyObserver(SignalConnectionBase connection) {
        super(connection);
    }

    @Override
    public void notify(Object sender, ConnectionHandle connectionHandle) {
        // ignore the connected flag
        boolean connected = isConnected(connectionHandle);
        ReconnectStrategy strategy = getReconnectStrategy();

        if (!connected && strategy != null) {
            Asserts.assertTrue(connectionHandle.getDisconnectFuture().isDone(), "The disconnectFuture must be done!");

            if (connectionHandle.getDisconnectFuture().isSuccess()) {
                boolean causedByNetwork = connectionHandle.disconnectedViaNetwork();
                if (causedByNetwork) {
                    strategy.start();
                }
            } else if (connectionHandle.getDisconnectFuture().isFailed()) {
                // a failed disconnect is as good as "causedByNetwork"
                strategy.start();
            }

        }
    }

}
