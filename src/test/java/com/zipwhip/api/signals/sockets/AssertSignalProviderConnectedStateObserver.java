package com.zipwhip.api.signals.sockets;

import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;

import static junit.framework.Assert.*;

/**
* Created with IntelliJ IDEA.
* User: Michael
* Date: 9/8/12
* Time: 2:24 PM
* To change this template use File | Settings | File Templates.
*/
public class AssertSignalProviderConnectedStateObserver implements Observer<ObservableFuture<ConnectionHandle>> {

    final SocketSignalProvider signalProvider;

    public AssertSignalProviderConnectedStateObserver(SocketSignalProvider signalProvider) {
        this.signalProvider = signalProvider;
    }

    @Override
    public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
        assertNotNull(item);
        SignalProviderTests.assertSuccess(item);
        assertSame(signalProvider.getCurrentConnectionHandle(), sender);
        assertTrue(signalProvider.getCurrentConnectionHandle() == sender);
        assertTrue(signalProvider.getCurrentConnectionHandle() == item.getResult());
        assertFalse(item.getResult().isDestroyed());
        assertFalse(item.getResult().getDisconnectFuture().isDone());
    }
}
