package com.zipwhip.concurrent;

import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.events.Observer;

import static junit.framework.Assert.assertTrue;

/**
* Created with IntelliJ IDEA.
* User: Michael
* Date: 9/8/12
* Time: 2:19 PM
* To change this template use File | Settings | File Templates.
*/
public class AssertDisconnectedStateFutureObserver implements Observer<ObservableFuture<ConnectionHandle>> {

    final boolean causedByNetwork;
    final SignalConnection signalConnection;

    public AssertDisconnectedStateFutureObserver(SignalConnection signalConnection, boolean causedByNetwork) {
        this.causedByNetwork = causedByNetwork;
        this.signalConnection = signalConnection;
    }

    public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
        TestUtil.assertSuccess(item);
        TestUtil.assertSuccess(item.getResult().getDisconnectFuture());
        TestUtil.assertSignalConnectionDisconnected(signalConnection, item.getResult(), causedByNetwork);
        assertTrue(item.getResult().isDestroyed());
    }
}
