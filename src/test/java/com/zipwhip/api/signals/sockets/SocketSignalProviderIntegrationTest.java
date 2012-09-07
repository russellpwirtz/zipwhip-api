package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.*;
import com.zipwhip.api.settings.MemorySettingStore;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalProviderTests;
import com.zipwhip.api.signals.SocketSignalProviderFactory;
import com.zipwhip.api.signals.reconnect.ExponentialBackoffReconnectStrategy;
import com.zipwhip.api.signals.sockets.netty.RawSocketIoChannelPipelineFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.concurrent.TestUtil;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.DownloadURL;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;
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

    private static final Logger LOGGER = Logger.getLogger(SocketSignalProviderIntegrationTest.class);

//    private String sessionKey = "6c20b056-6843-404d-9fb4-b492d54efe75:142584301"; // evo 3d
//    private String sessionKey = "fc3890ba-a2c7-4449-a4c7-c80f57af228b:142584301"; // evo 3d
//    private String host = "http://network.zipwhip.com";

    ZipwhipClient client;
    SocketSignalProvider signalProvider;

    @Before
    public void setUp() throws Exception {
        ApiConnectionConfiguration.API_HOST = ApiConnection.STAGING_HOST;
        ApiConnectionConfiguration.SIGNALS_HOST = ApiConnection.STAGING_SIGNALS_HOST;

        ApiConnectionFactory connectionFactory = ApiConnectionFactory.newInstance();
//                .sessionKey(sessionKey);

//        connectionFactory.setUsername("9139802972");
//        connectionFactory.setPassword("asdfasdf");

        // staging
        connectionFactory.setPassword("pistons456");
        connectionFactory.setUsername("2062513225");

        SocketSignalProviderFactory signalProviderFactory = SocketSignalProviderFactory.newInstance()
                .reconnectStrategy(new ExponentialBackoffReconnectStrategy())
                .channelPipelineFactory(new RawSocketIoChannelPipelineFactory(60, 5));

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, signalProviderFactory);

        ZipwhipClient zipwhipClient = zipwhipClientFactory.create();

        zipwhipClient.setSettingsStore(new MemorySettingStore());

        client = zipwhipClient;
        signalProvider = (SocketSignalProvider)client.getSignalProvider();
    }

    @Test
    public void testConnectAndVerify() throws Exception {
        ConnectionHandle connectionHandle = TestUtil.awaitAndAssertSuccess(client.connect());
        assertFalse("connecting returned null?", connectionHandle.isDestroyed());

        String sessionKey = client.getConnection().getSessionKey();
        assertTrue(client.getSignalProvider().getClientId() != null);
        assertTrue(client.getSignalProvider().getConnectionState() == ConnectionState.AUTHENTICATED);

        final String requestId = UUID.randomUUID().toString();
        int index = new Random().nextInt();

        final CountDownLatch latch = new CountDownLatch(1);

        final Signal[] verifySignal = new Signal[1];

        client.getSignalProvider().getSignalReceivedEvent().addObserver(new Observer<List<Signal>>() {
            @Override
            public void notify(Object sender, List<Signal> item) {
                for (Signal signal : item) {
                    if (requestId.equals(signal.getType())) {
                        verifySignal[0] = signal;
                    }
                }
                latch.countDown();
            }
        });

        assertEquals(client.getConnection().getSessionKey(), sessionKey);
        assertTrue(client.getSignalProvider().getClientId() != null);
        assertTrue(client.getSignalProvider().getConnectionState() == ConnectionState.AUTHENTICATED);

        LOGGER.debug(DownloadURL.get("http://staging.zipwhip.com/mvc/signals/signal?session=" + sessionKey + "&requestId=" + requestId + "&type=" + requestId + "&scope=" + index));

        assertTrue("Latch didn't finish?", latch.await(50, TimeUnit.SECONDS));

        assertNotNull(verifySignal[0]);
    }


    @Test
    public void testBasicConnect() throws Exception {
        final ConnectionHandle connectionHandle = TestUtil.awaitAndAssertSuccess(signalProvider.connect());

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



    @Test
    public void testBasicConnect2() throws Exception {
        ObservableFuture<ConnectionHandle> future = signalProvider.connect();

        TestUtil.awaitAndAssertSuccess(future);

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

        TestUtil.awaitAndAssertSuccess(signalProvider.disconnect());
        assertTrue(connectionHandle.isDestroyed());
        assertFalse(signalProvider.getConnectionState() == ConnectionState.CONNECTED);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testConnectDisconnect() throws Exception {
        ObservableFuture<ConnectionHandle> future = signalProvider.connect();

        future.addObserver(new SignalProviderTests.AssertConnectedStateObserver(signalProvider));

        TestUtil.awaitAndAssertSuccess(future);
        assertNotNull((signalProvider).getCurrentConnection());

        TestUtil.awaitAndAssertSuccess(signalProvider.disconnect());
        assertNull("Current connection must be null after a synchronous disconnect", (signalProvider).getCurrentConnection());
    }

    @After
    public void tearDown() throws Exception {
        ApiConnectionConfiguration.API_HOST = ApiConnection.DEFAULT_HTTPS_HOST;
        ApiConnectionConfiguration.SIGNALS_HOST = ApiConnection.DEFAULT_SIGNALS_HOST;
    }
}
