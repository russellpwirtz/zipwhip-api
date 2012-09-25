package com.zipwhip.api.signals.sockets;

import com.zipwhip.concurrent.FakeFailingObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;

/**
 * Created with IntelliJ IDEA.
 * User: msmyers
 * Date: 9/4/12
 * Time: 5:44 PM
 *
 * This is used to say: "we're not connected right now"
 */
public class MockSignalProviderConnectionHandle extends SignalProviderConnectionHandle {

    public MockSignalProviderConnectionHandle() {
        super(0, null);
    }

    @Override
    public long getId() {
        return -1;
    }

    @Override
    public ObservableFuture<ConnectionHandle> reconnect() {
        return new FakeFailingObservableFuture<ConnectionHandle>(this, new IllegalStateException("Not connected"));
    }

    @Override
    public boolean isFor(ConnectionHandle connectionHandle) {
        return false;
    }

}
