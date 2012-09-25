package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.sockets.*;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.concurrent.TestUtil;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;
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
        signalProvider = new SocketSignalProvider(new MockSignalConnection(), SimpleExecutor.getInstance());
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
                assertFalse(signalProvider.getConnectionState() == ConnectionState.CONNECTED);
                assertFalse(signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);
                assertTrue(connectionHandle.isDestroyed());
                latch.countDown();
            }
        });

        signalProvider.getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean connected) {
                assertFalse(connected);
                assertFalse(signalProvider.getConnectionState() == ConnectionState.CONNECTED);
                assertFalse(signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);
                assertTrue(connectionHandle.isDestroyed());
                latch.countDown();
            }
        });

        assertTrue(signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);

        TestUtil.awaitAndAssertSuccess(connectionHandle.disconnect());
        assertTrue(connectionHandle.isDestroyed());
        assertFalse(signalProvider.getConnectionState() == ConnectionState.CONNECTED);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }



    @Test
    public void testBasicConnect2() throws Exception {

        ObservableFuture<ConnectionHandle> future = signalProvider.connect();

        future.await();

        assertSuccess(future);

        // because of threading we have to wait a bit.

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
                assertFalse(signalProvider.getConnectionState() == ConnectionState.CONNECTED);
                assertFalse(signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);
                assertTrue(connectionHandle.isDestroyed());
                latch.countDown();
            }
        });

        signalProvider.getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean connected) {
                assertFalse(connected);
                assertFalse(signalProvider.getConnectionState() == ConnectionState.CONNECTED);
                assertFalse(signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);
                assertTrue(connectionHandle.isDestroyed());
                latch.countDown();
            }
        });

        assertTrue(signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);

        TestUtil.awaitAndAssertSuccess(signalProvider.disconnect());
        assertTrue(connectionHandle.isDestroyed());
        assertFalse(signalProvider.getConnectionState() == ConnectionState.CONNECTED);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testConnectDisconnect() throws Exception {
        ObservableFuture<ConnectionHandle> future = signalProvider.connect();

        future.addObserver(new AssertSignalProviderConnectedStateObserver(signalProvider));

        future.await();

        assertSuccess(future);
        assertNotNull((signalProvider).getCurrentConnectionHandle());

        future = signalProvider.disconnect();

        TestUtil.awaitAndAssertSuccess(future);
        assertNull((signalProvider).getCurrentConnectionHandle());
    }

    public static <T> void assertSuccess(ObservableFuture<T> future) {
        assertTrue(future.isDone());
        assertTrue("Future was not successful? " + future.getCause(), future.isSuccess());
        assertFalse(future.isCancelled());
        assertFalse(future.isFailed());
    }

}
