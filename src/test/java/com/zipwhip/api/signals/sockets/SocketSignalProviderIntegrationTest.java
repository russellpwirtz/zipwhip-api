package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ApiConnectionConfiguration;
import com.zipwhip.api.signals.SocketSignalProviderFactory;
import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.sockets.netty.RawSocketIoChannelPipelineFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.concurrent.TestUtil;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.reliable.retry.ExponentialBackoffRetryStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/28/12
 * Time: 1:51 PM
 */
public class SocketSignalProviderIntegrationTest {

    SocketSignalProvider signalProvider;

    @Before
    public void setUp() throws Exception {
        ApiConnectionConfiguration.API_HOST = ApiConnection.STAGING_HOST;
        ApiConnectionConfiguration.SIGNALS_HOST = ApiConnection.STAGING_SIGNALS_HOST;

        SocketSignalProviderFactory signalProviderFactory = SocketSignalProviderFactory.newInstance()
                .reconnectStrategy(new DefaultReconnectStrategy(null, new ExponentialBackoffRetryStrategy(1000, 2.0)))
                .channelPipelineFactory(new RawSocketIoChannelPipelineFactory(60, 5));

        signalProvider = (SocketSignalProvider)signalProviderFactory.create();
    }

    @Ignore
    @Test
    public void testBasicConnect() throws Exception {
        final ConnectionHandle connectionHandle = TestUtil.awaitAndAssertSuccess(signalProvider.connect());

        // let the events die down. (The rule is to notify observers after the future finishes!
        Thread.sleep(100);

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
                TestUtil.awaitAndAssertSuccess(item);
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

    @Ignore
    @Test
    public void testBasicConnect2() throws Exception {

        final ConnectionHandle[] connectionHandles = new ConnectionHandle[1];
        final CountDownLatch latch = new CountDownLatch(3);
        signalProvider.getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean connected) {
                if (!connected) {
                    assertFalse(signalProvider.getConnectionState() == ConnectionState.CONNECTED);
                    assertFalse(signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);
                    assertTrue(connectionHandles[0].isDestroyed());
                    latch.countDown();
                }
            }
        });

        ObservableFuture<ConnectionHandle> future = signalProvider.connect();

        final ConnectionHandle connectionHandle = TestUtil.awaitAndAssertSuccess(future);
        connectionHandles[0] = connectionHandle;

        connectionHandle.link(new DestroyableBase() {
            @Override
            protected void onDestroy() {
                latch.countDown();
            }
        });

        connectionHandle.getDisconnectFuture().addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                TestUtil.awaitAndAssertSuccess(item);
                assertNotNull(item.getResult());
                assertFalse(signalProvider.getConnectionState() == ConnectionState.CONNECTED);
                assertFalse(signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);
                assertTrue(connectionHandles[0].isDestroyed());
                assertSame(connectionHandle, connectionHandles[0]);
                assertSame(item.getResult(), connectionHandles[0]);
                assertSame(sender, connectionHandles[0]);
                latch.countDown();
            }
        });


        assertTrue(signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);

        TestUtil.awaitAndAssertSuccess(signalProvider.disconnect());
        assertTrue(connectionHandle.isDestroyed());
        assertFalse(signalProvider.getConnectionState() == ConnectionState.CONNECTED);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Ignore
    @Test
    public void testConnectDisconnect() throws Exception {
        ObservableFuture<ConnectionHandle> future = signalProvider.connect();

        future.addObserver(new AssertSignalProviderConnectedStateObserver(signalProvider));

        TestUtil.awaitAndAssertSuccess(future);
        assertNotNull((signalProvider).getCurrentConnectionHandle());

        TestUtil.awaitAndAssertSuccess(signalProvider.disconnect());
        assertNull("Current connection must be null after a synchronous disconnect", (signalProvider).getCurrentConnectionHandle());
    }

    @After
    public void tearDown() throws Exception {
        if (signalProvider != null){
            signalProvider.destroy();
        }
        ApiConnectionConfiguration.API_HOST = ApiConnection.DEFAULT_HTTPS_HOST;
        ApiConnectionConfiguration.SIGNALS_HOST = ApiConnection.DEFAULT_SIGNALS_HOST;
    }
}
