package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.concurrent.ObservableFuture;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/8/11
 * Time: 3:55 PM
 */
public class MockReconnectStrategy extends ReconnectStrategy {

    @Override
    protected void doStrategyWithoutBlocking() {

        try {
            System.out.println("Strategy firing...");
            ObservableFuture<ConnectionHandle> reconnectTask = signalConnection.connect();
            ConnectionHandle connectionHandle = reconnectTask.get();

            if (connectionHandle == null || (connectionHandle.isDestroyed() && connectionHandle.disconnectedViaNetwork())) {
                signalConnection.disconnect(true);
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void onDestroy() {
        stop();
    }
}
