package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.api.signals.sockets.MockSignalConnection;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.concurrent.TestUtil;
import com.zipwhip.events.MockObserver;
import com.zipwhip.events.Observer;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertFalse;

/**
 * Created with IntelliJ IDEA.
 * User: msmyers
 * Date: 9/5/12
 * Time: 4:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class MockConnectionTests {

    SignalConnection signalConnection;

    @Before
    public void setUp() throws Exception {
        this.signalConnection = new MockSignalConnection();

    }

    @Test
    public void testReconnect1() throws InterruptedException {
        ConnectionHandle connectionHandle = TestUtil.awaitAndAssertSuccess(signalConnection.connect());
        ConnectionHandle connectionHandle1 = TestUtil.awaitAndAssertSuccess(connectionHandle.reconnect());

        assertTrue(connectionHandle.isDestroyed());
        assertFalse(connectionHandle1.isDestroyed());

        ConnectionHandle connectionHandle2 = TestUtil.awaitAndAssertSuccess(connectionHandle1.disconnect());

        assertSame(connectionHandle1, connectionHandle2);
    }

    @Test
    public void testReconnect2() throws InterruptedException {
        ConnectionHandle connectionHandle = TestUtil.awaitAndAssertSuccess(signalConnection.connect());
        ConnectionHandle connectionHandle1 = TestUtil.awaitAndAssertSuccess(signalConnection.reconnect());

        assertTrue(connectionHandle.isDestroyed());
        assertFalse(connectionHandle1.isDestroyed());

        ConnectionHandle connectionHandle2 = TestUtil.awaitAndAssertSuccess(connectionHandle1.disconnect());

        assertSame(connectionHandle1, connectionHandle2);
    }

    @Test
    public void testDisconnectFuture() throws Exception {

        ConnectionHandle connectionHandle = connect();

        MockObserver<ObservableFuture<ConnectionHandle>> observer = new MockObserver<ObservableFuture<ConnectionHandle>>();
        connectionHandle.getDisconnectFuture().addObserver(observer);

        signalConnection.getDisconnectEvent().addObserver(new AssertDisconnectedStateObserver(false));
        connectionHandle.getDisconnectFuture().addObserver(new AssertDisconnectedStateFutureObserver(false));

        assertFalse(observer.isCalled());

        connectionHandle.disconnect().await();

        assertTrue(observer.isCalled());
        assertFalse(connectionHandle.disconnectedViaNetwork());
    }

    @Test
    public void testDisconnectFuture1() throws Exception {

        ConnectionHandle connectionHandle = connect();

        MockObserver<ObservableFuture<ConnectionHandle>> observer = new MockObserver<ObservableFuture<ConnectionHandle>>();
        connectionHandle.getDisconnectFuture().addObserver(observer);

        signalConnection.getDisconnectEvent().addObserver(new AssertDisconnectedStateObserver(true));
        connectionHandle.getDisconnectFuture().addObserver(new AssertDisconnectedStateFutureObserver(true));

        assertFalse(observer.isCalled());

        connectionHandle.disconnect(true).await();

        assertTrue(observer.isCalled());
        assertTrue(connectionHandle.disconnectedViaNetwork());
        assertNull("Current connection null because disconnected", signalConnection.getConnectionHandle());
    }

    @Test
    public void testDisconnectFuture2() throws Exception {
        ConnectionHandle connectionHandle = connect();
        MockObserver<ObservableFuture<ConnectionHandle>> observer = new MockObserver<ObservableFuture<ConnectionHandle>>();
        connectionHandle.getDisconnectFuture().addObserver(observer);
        connectionHandle.getDisconnectFuture().addObserver(new AssertDisconnectedStateFutureObserver(false));
        signalConnection.getDisconnectEvent().addObserver(new AssertDisconnectedStateObserver(false));

        assertFalse(observer.isCalled());

        signalConnection.disconnect().await();

        assertTrue("getDisconnectFuture() wasn't called on signalConnection.disconnect()", observer.isCalled());
        assertSignalConnectionDisconnected(connectionHandle, false);
    }

    @Test
    public void testDisconnectFuture3() throws Exception {
        ConnectionHandle connectionHandle = connect();
        MockObserver<ObservableFuture<ConnectionHandle>> observer = new MockObserver<ObservableFuture<ConnectionHandle>>();
        connectionHandle.getDisconnectFuture().addObserver(observer);

        signalConnection.getDisconnectEvent().addObserver(new AssertDisconnectedStateObserver(true));
        connectionHandle.getDisconnectFuture().addObserver(new AssertDisconnectedStateFutureObserver(true));

        signalConnection.getDisconnectEvent().addObserver(new Observer<ConnectionHandle>() {
            @Override
            public void notify(Object sender, ConnectionHandle item) {
                assertTrue(item.isDestroyed());
                assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
                assertNull(signalConnection.getConnectionHandle());
            }
        });

        assertFalse(observer.isCalled());

        signalConnection.disconnect(true).await();

        assertTrue("getDisconnectFuture() wasn't called on signalConnection.disconnect()", observer.isCalled());
        assertTrue("Disconnected via network", connectionHandle.disconnectedViaNetwork());
    }

    @Test
    public void testDisconnectWhileConnecting() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        signalConnection.getConnectEvent().addObserver(new Observer<ConnectionHandle>() {
            @Override
            public void notify(Object sender, ConnectionHandle item) {

                assertTrue(!item.isDestroyed());

                item.disconnect().addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
                    @Override
                    public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                        assertSuccess(item);
                        boolean causedByNetwork = item.getResult().disconnectedViaNetwork();
                        assertFalse(causedByNetwork);

                        latch.countDown();
                    }
                });
            }
        });

        signalConnection.getDisconnectEvent().addObserver(new AssertDisconnectedStateObserver(false));

        ObservableFuture<ConnectionHandle> future = signalConnection.connect();

        future.await();

        assertTrue("Didn't connect?", future.isSuccess());
        assertNotNull("Result null?", future.getResult());

        future.getResult().getDisconnectFuture().addObserver(new AssertDisconnectedStateFutureObserver(false));

        latch.await();

        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
    }

    @Test
    public void testConnectedEventHasRightState() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final ConnectionHandle[] otherConnectionHandle = new ConnectionHandle[1];

        signalConnection.getConnectEvent().addObserver(new Observer<ConnectionHandle>() {
            @Override
            public void notify(Object sender, ConnectionHandle item) {
                otherConnectionHandle[0] = item;
                assertFalse(item.isDestroyed());
                assertTrue("State was " + signalConnection.getConnectionState(),
                        signalConnection.getConnectionState() == ConnectionState.CONNECTED);
                latch.countDown();
            }
        });

        ObservableFuture<ConnectionHandle> future = signalConnection.connect();

        latch.await();
        future.await();

        assertTrue(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
        assertFalse(signalConnection.isDestroyed());
        assertFalse(future.getResult().isDestroyed());
        assertTrue(otherConnectionHandle[0] == future.getResult());

    }



    @Test
    public void testSimpleConnect() throws Exception {
        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
        ObservableFuture<ConnectionHandle> future = signalConnection.connect();

        future.await();

        ConnectionHandle conn = future.getResult();

        assertTrue(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
        assertTrue(!conn.isDestroyed());
        assertNotDone(conn.getDisconnectFuture());
    }

    @Test
    public void testReconnect() throws Exception {
        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        ObservableFuture<ConnectionHandle> future1 = signalConnection.connect();

        future1.await();

        ConnectionHandle conn1 = future1.getResult();

        signalConnection.getDisconnectEvent().addObserver(new AssertDisconnectedStateObserver(false));
        conn1.getDisconnectFuture().addObserver(new AssertDisconnectedStateFutureObserver(false));

        assertTrue(!conn1.isDestroyed());
        assertNotDone(conn1.getDisconnectFuture());

        ObservableFuture<ConnectionHandle> future2 = conn1.reconnect();

        future2.await();

        assertTrue("Was not connected?", signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        ConnectionHandle conn2 = future2.getResult();

        assertTrue("Connection2 should be active", !conn2.isDestroyed());
        assertFalse(!conn1.isDestroyed());

        assertSuccess(conn1.getDisconnectFuture());
        assertNotDone(conn2.getDisconnectFuture());
    }

    @Test
    public void testDisconnect() throws Exception {
        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        ObservableFuture<ConnectionHandle> future1 = signalConnection.connect();

        future1.await();

        ConnectionHandle conn1 = future1.getResult();

        signalConnection.getDisconnectEvent().addObserver(new AssertDisconnectedStateObserver(false));
        conn1.getDisconnectFuture().addObserver(new AssertDisconnectedStateFutureObserver(false));

        assertTrue(!conn1.isDestroyed());
        assertNotDone(conn1.getDisconnectFuture());

        assertTrue(signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        ObservableFuture<ConnectionHandle> future2 = conn1.disconnect();

        future2.await();

        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        Boolean causedByNetwork = future2.getResult().disconnectedViaNetwork();

        assertFalse(causedByNetwork);
        assertFalse(!conn1.isDestroyed());
        assertSuccess(conn1.getDisconnectFuture());
    }

    @Test
    public void testDisconnectViaNetwork() throws Exception {
        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        ObservableFuture<ConnectionHandle> future1 = signalConnection.connect();

        future1.await();

        ConnectionHandle conn1 = future1.getResult();

        signalConnection.getDisconnectEvent().addObserver(new AssertDisconnectedStateObserver(true));
        conn1.getDisconnectFuture().addObserver(new AssertDisconnectedStateFutureObserver(true));

        assertTrue(!conn1.isDestroyed());
        assertNotDone(conn1.getDisconnectFuture());
        assertTrue(signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        ObservableFuture<ConnectionHandle> future2 = conn1.disconnect(true);

        future2.await();

        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        Boolean causedByNetwork = future2.getResult().disconnectedViaNetwork();

        assertTrue(causedByNetwork);
        assertFalse(!conn1.isDestroyed());
        assertSuccess(conn1.getDisconnectFuture());
    }

    @Test
    public void testDisconnectMatching() throws Exception {
        ObservableFuture<ConnectionHandle> future = signalConnection.connect();

        future.await();
        assertSuccess(future);

        ConnectionHandle connectionHandle = future.getResult();

        assertNotDone(connectionHandle.getDisconnectFuture());

        assertFalse(connectionHandle.isDestroyed());

        ObservableFuture<ConnectionHandle> disconnectFuture = signalConnection.disconnect();

        disconnectFuture.await();
        assertSuccess(disconnectFuture);

        assertSame(connectionHandle, disconnectFuture.getResult());
        assertTrue(connectionHandle.isDestroyed());
        assertSuccess(connectionHandle.getDisconnectFuture());
        // causedByNetwork
        assertFalse(connectionHandle.getDisconnectFuture().getResult().disconnectedViaNetwork());
    }

    public void assertSuccess(ObservableFuture<?> future) {
        assertNotNull(future);
        assertTrue(future.isDone());
        assertTrue(future.isSuccess());
        assertFalse(future.isCancelled());
        assertFalse(future.isFailed());
        assertNotNull(future.getResult());
    }

    private void assertNotDone(ObservableFuture<?> future) {
        assertNotNull(future);
        assertFalse(future.isDone());
        assertFalse(future.isSuccess());
        assertFalse(future.isCancelled());
        assertFalse(future.isFailed());
        assertNull(future.getResult());
    }

    public class AssertDisconnectedStateObserver implements Observer<ConnectionHandle> {

        final boolean causedByNetwork;

        public AssertDisconnectedStateObserver(boolean causedByNetwork) {
            this.causedByNetwork = causedByNetwork;
        }

        @Override
        public void notify(Object sender, ConnectionHandle item) {
            assertSuccess(item.getDisconnectFuture());
            assertSame(item.disconnectedViaNetwork(), causedByNetwork);
            assertTrue(item.isDestroyed());
        }
    }

    protected class AssertDisconnectedStateFutureObserver implements Observer<ObservableFuture<ConnectionHandle>> {

        final boolean causedByNetwork;

        public AssertDisconnectedStateFutureObserver(boolean causedByNetwork) {
            this.causedByNetwork = causedByNetwork;
        }

        public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
            assertSuccess(item);
            assertSuccess(item.getResult().getDisconnectFuture());
            assertSignalConnectionDisconnected(item.getResult(), causedByNetwork);
            assertTrue(item.getResult().isDestroyed());
        }
    }

    private void assertSignalConnectionDisconnected(ConnectionHandle connectionHandle, boolean causedByNetwork) {
        if (connectionHandle != null) {
            assertTrue(connectionHandle.isDestroyed());
            assertSame("Network values must agree", connectionHandle.disconnectedViaNetwork(), causedByNetwork);
            assertSuccess(connectionHandle.getDisconnectFuture());
        }

        assertNull("Current connection null because disconnected", signalConnection.getConnectionHandle());
        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
    }

    public ConnectionHandle connect() throws Exception {
        ObservableFuture<ConnectionHandle> future = signalConnection.connect();

        future.await();

        ConnectionHandle connectionHandle = future.getResult();

        return connectionHandle;
    }

}
