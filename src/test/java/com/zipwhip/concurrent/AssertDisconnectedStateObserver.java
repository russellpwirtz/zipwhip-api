package com.zipwhip.concurrent;

import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.events.Observer;

import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

/**
* Created with IntelliJ IDEA.
* User: Michael
* Date: 9/8/12
* Time: 2:20 PM
* To change this template use File | Settings | File Templates.
*/
public class AssertDisconnectedStateObserver implements Observer<ConnectionHandle> {

    final boolean causedByNetwork;

    public AssertDisconnectedStateObserver(boolean causedByNetwork) {
        this.causedByNetwork = causedByNetwork;
    }

    @Override
    public void notify(Object sender, ConnectionHandle item) {
        TestUtil.assertSuccess(item.getDisconnectFuture());
        assertSame(item.disconnectedViaNetwork(), causedByNetwork);
        assertTrue(item.isDestroyed());
    }
}
