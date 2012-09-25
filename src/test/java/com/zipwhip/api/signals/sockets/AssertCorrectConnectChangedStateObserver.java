package com.zipwhip.api.signals.sockets;

import com.zipwhip.events.Observer;

import static com.zipwhip.concurrent.TestUtil.assertDisconnected;
import static junit.framework.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/8/12
 * Time: 2:36 PM
 *
 *
 */
public class AssertCorrectConnectChangedStateObserver implements Observer<Boolean> {

    final SocketSignalProvider signalProvider;

    public AssertCorrectConnectChangedStateObserver(SocketSignalProvider signalProvider) {
        this.signalProvider = signalProvider;
    }

    @Override
    public void notify(Object sender, Boolean connected) {
        ConnectionHandle connectionHandle = (ConnectionHandle) sender;

        if (!connected) {
            assertDisconnected(connectionHandle);
            assertDisconnected(signalProvider);
        } else {
            assertConnected(connectionHandle);
            assertConnected(signalProvider);
        }
    }

    public static ConnectionHandle assertConnected(SocketSignalProvider signalProvider) {
        final ConnectionHandle connectionHandle = signalProvider.getCurrentConnectionHandle();

        assertTrue(signalProvider.isConnected());
        assertTrue(signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);
        assertNotNull(signalProvider.getCurrentConnectionHandle());

        assertSame(signalProvider.getCurrentConnectionHandle(), connectionHandle);
        return connectionHandle;
    }

    private void assertConnected(ConnectionHandle connectionHandle) {
        assertNotNull(connectionHandle);
        assertFalse(connectionHandle.isDestroyed());
        assertFalse(connectionHandle.getDisconnectFuture().isDone());

    }
}
