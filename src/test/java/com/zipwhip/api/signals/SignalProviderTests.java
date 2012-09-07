package com.zipwhip.api.signals;

import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.MockSignalConnection;
import com.zipwhip.api.signals.sockets.SocketSignalProvider;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.concurrent.TestUtil;
import com.zipwhip.events.Observer;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.lifecycle.DestroyableBase;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: msmyers
 * Date: 9/5/12
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class SignalProviderTests {

    SocketSignalProvider signalProvider;

    @Before
    public void setUp() throws Exception {
        signalProvider = new SocketSignalProvider(new MockSignalConnection(), new ImportantTaskExecutor(), null);
    }

    @Test
    public void testBasicConnect() throws Exception {
        ObservableFuture<ConnectionHandle> future = signalProvider.connect();

        future.await();

        assertSuccess(future);

        final ConnectionHandle connectionHandle = future.getResult();
        final CountDownLatch latch = new CountDownLatch(3);

        connectionHandle.link(new DestroyableBase() {
            @Override
            protected void onDestroy() {
                latch.countDown();
            }
        });

        connectionHandle.getDisconnectFuture().addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                assertSuccess(item);
                assertNotNull(item.getResult());
                assertFalse(signalProvider.isConnected());
                assertFalse(signalProvider.isAuthenticated());
                assertTrue(connectionHandle.isDestroyed());
                latch.countDown();
            }
        });

        signalProvider.getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean connected) {
                assertFalse(connected);
                assertFalse(signalProvider.isConnected());
                assertFalse(signalProvider.isAuthenticated());
                assertTrue(connectionHandle.isDestroyed());
                latch.countDown();
            }
        });

        assertTrue(signalProvider.isConnected());

        TestUtil.awaitAndAssertSuccess(connectionHandle.disconnect());
        assertTrue(connectionHandle.isDestroyed());
        assertFalse(signalProvider.isConnected());

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }



    @Test
    public void testBasicConnect2() throws Exception {
        ObservableFuture<ConnectionHandle> future = signalProvider.connect();

        future.await();

        assertSuccess(future);

        final ConnectionHandle connectionHandle = future.getResult();
        final CountDownLatch latch = new CountDownLatch(3);

        connectionHandle.link(new DestroyableBase() {
            @Override
            protected void onDestroy() {
                latch.countDown();
            }
        });

        connectionHandle.getDisconnectFuture().addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                assertSuccess(item);
                assertNotNull(item.getResult());
                assertFalse(signalProvider.isConnected());
                assertFalse(signalProvider.isAuthenticated());
                assertTrue(connectionHandle.isDestroyed());
                latch.countDown();
            }
        });

        signalProvider.getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean connected) {
                assertFalse(connected);
                assertFalse(signalProvider.isConnected());
                assertFalse(signalProvider.isAuthenticated());
                assertTrue(connectionHandle.isDestroyed());
                latch.countDown();
            }
        });

        assertTrue(signalProvider.isConnected());

        TestUtil.awaitAndAssertSuccess(signalProvider.disconnect());
        assertTrue(connectionHandle.isDestroyed());
        assertFalse(signalProvider.isConnected());

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testConnectDisconnect() throws Exception {
        ObservableFuture<ConnectionHandle> future = signalProvider.connect();

        future.addObserver(new AssertConnectedStateObserver(signalProvider));

        future.await();

        assertSuccess(future);
        assertNotNull((signalProvider).getCurrentConnection());

        future = signalProvider.disconnect();

        TestUtil.awaitAndAssertSuccess(future);
        assertNull((signalProvider).getCurrentConnection());
    }

    public static <T> void assertSuccess(ObservableFuture<T> future) {
        assertTrue(future.isDone());
        assertTrue("Future was not successful? " + future.getCause(), future.isSuccess());
        assertFalse(future.isCancelled());
        assertFalse(future.isFailed());
    }

    public static class AssertConnectedStateObserver implements Observer<ObservableFuture<ConnectionHandle>> {

        final SocketSignalProvider signalProvider;

        public AssertConnectedStateObserver(SocketSignalProvider signalProvider) {
            this.signalProvider = signalProvider;
        }

        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
            assertNotNull(item);
            assertSuccess(item);
            assertSame(signalProvider.getCurrentConnection(), sender);
            assertTrue(signalProvider.getCurrentConnection() == sender);
            assertTrue(signalProvider.getCurrentConnection() == item.getResult());
            assertFalse(item.getResult().isDestroyed());
            assertFalse(item.getResult().getDisconnectFuture().isDone());
        }
    }
}
