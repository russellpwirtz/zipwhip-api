package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.ReconnectStrategy;
import com.zipwhip.api.signals.SignalConnection;

import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/8/11
 * Time: 3:55 PM
 */
public class MockReconnectStrategy extends ReconnectStrategy {

    public MockReconnectStrategy(SignalConnection signalConnection) {
        super(signalConnection);
    }

    @Override
    protected void doStrategy() {

        try {
            System.out.println("Strategy firing...");
            Future<Boolean> reconnectTask = signalConnection.connect();
            Boolean success = reconnectTask.get();

            if (!success) {
                signalConnection.disconnect(true);
            }
        } catch (Exception e) {
        }
    }

}
