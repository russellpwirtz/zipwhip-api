package com.zipwhip.api;

import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.response.StringServerResponse;
import com.zipwhip.api.settings.MemorySettingStore;
import com.zipwhip.api.signals.MockSignalProvider;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.api.signals.sockets.*;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.concurrent.TestUtil;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.NullExecutor;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.SignTool;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/18/11
 * Time: 11:11 AM
 */
public class DefaultZipwhipClientTest {

    private static final Logger LOGGER = Logger.getLogger(DefaultZipwhipClientTest.class);

    ZipwhipClient client;
    SignalProvider signalProvider;
    ApiConnection apiConnection;

    public final static String MOBILE_NUMBER = "2069797502";

    @Before
    public void setUp() throws Exception {
        apiConnection = new MockApiConnection();
        signalProvider = new MockSignalProvider();
        client = new DefaultZipwhipClient(null, null, apiConnection, signalProvider);
        ((DefaultZipwhipClient) client).signalsConnectTimeoutInSeconds = 5;
        client.setSettingsStore(new MemorySettingStore());
    }

    @After
    public void tearDown() {
        client.getSettingsStore().clear();
    }

    @Test
    public void testSimpleConnect() throws Exception {
        ObservableFuture<ConnectionHandle> future = client.connect();

        ConnectionHandle handle = TestUtil.awaitAndAssertSuccess(future);

        assertFalse(handle.isDestroyed());
        assertTrue("Client not connected", client.isConnected());
    }

    @Test
    public void testDisconnectConnectDisconnect() throws Exception {

        ObservableFuture<ConnectionHandle> connectFuture1 = client.connect();
        TestUtil.awaitAndAssertSuccess(connectFuture1);

        ObservableFuture<ConnectionHandle> disconnectFuture1 = client.disconnect();
        TestUtil.awaitAndAssertSuccess(disconnectFuture1);

        ObservableFuture<ConnectionHandle> connectFuture2 = client.connect();
        assertFalse("The two futures cannot be the same!", connectFuture1 == connectFuture2);
        TestUtil.awaitAndAssertSuccess(connectFuture2);

        ObservableFuture<ConnectionHandle> disconnectFuture2 = client.disconnect();
        assertFalse(disconnectFuture1 == disconnectFuture2);
        TestUtil.awaitAndAssertSuccess(disconnectFuture2);
    }

    // connect
    // callback
    // latch
    // close channel (close from bottom up)
    // result: all client events fire in client executor
    // result:

    @Test
    public void testConnectDisconnectNoDeadlock() throws Exception {

        final int RUN_COUNT = 100;
        final boolean[] hasErrors = {false};
        final CountDownLatch latch = new CountDownLatch(RUN_COUNT - 1);

        class ConnectOrDisconnectBasedOnIterationNumberRunnable implements Runnable {

            int iteration;

            public ConnectOrDisconnectBasedOnIterationNumberRunnable(Integer iteration) {
                this.iteration = iteration;
            }

            @Override
            public void run() {
                try {
                    System.out.println("Connecting at iteration " + iteration);
                    ObservableFuture<ConnectionHandle> future = client.connect();
                    assertTrue(future.await(2, TimeUnit.SECONDS));
                } catch (Exception e) {
                    LOGGER.debug("Exception ", e);
                    hasErrors[0] = true;
                }
                try {
                    System.out.println("Disconnecting at iteration " + iteration);
                    ObservableFuture<ConnectionHandle> future = client.disconnect();
                    assertTrue(future.await(2, TimeUnit.SECONDS));
                } catch (Exception e) {
                    LOGGER.debug("Exception ", e);
                    hasErrors[0] = true;
                }

                latch.countDown();
            }
        }

        ExecutorService executor = Executors.newCachedThreadPool();

        for (int i = 0; i < RUN_COUNT; i++) {
            executor.execute(new ConnectOrDisconnectBasedOnIterationNumberRunnable(i));
        }


        assertTrue("All finished", latch.await(10, TimeUnit.SECONDS));

        assertFalse("Has errors", hasErrors[0]);

        executor.shutdown();
    }


    @Test
    public void testConnect() throws Exception {
        ((DefaultZipwhipClient)client).setSignalsConnectTimeoutInSeconds(9000);
        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);

        ObservableFuture<ConnectionHandle> future = client.connect();

        // ensure that we're connected
        assertNotNull("Was not connected?", TestUtil.awaitAndAssertSuccess(future));

        assertTrue("Client wasn't connected!", client.isConnected());
        assertTrue(client.getSignalProvider().getConnectionState() == ConnectionState.AUTHENTICATED);

        assertEquals(1, connectionChangedObserver.connectionChangedEvents);
        assertTrue(connectionChangedObserver.connected);
    }

    @Test
    public void testConnectSignalConnectFailure() throws Exception {

        ((MockApiConnection) apiConnection).failSignalsConnect = true;

        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        client.getSignalProvider().getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                if (Boolean.FALSE.equals(item))
                    latch2.countDown();
                else
                    latch1.countDown();
            }
        });

        ObservableFuture<ConnectionHandle> future = client.connect();

        assertTrue("Didn't finish!?", future.await(5, TimeUnit.SECONDS));
        assertTrue(future.isDone());
        assertFalse(future.isSuccess());

        // connected first.
        assertTrue(latch1.await(5, TimeUnit.SECONDS));

        // disconnected next.
        assertTrue(latch2.await(5, TimeUnit.SECONDS));

        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse("Should not be connected", connectionChangedObserver.connected);
    }

    @Test
    public void testConnectSignalConnectException() throws Exception {

        ((MockApiConnection) apiConnection).failSignalsConnectWithException = true;

        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);

        final CountDownLatch latch = new CountDownLatch(1);
        client.getSignalProvider().getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                if (item.equals(Boolean.FALSE))
                    latch.countDown();
            }
        });

        client.connect().get(5, TimeUnit.SECONDS);

        latch.await(5, TimeUnit.SECONDS);

        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testDisconnect() throws Exception {
        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);

        assertTrue(client.connect().await(4, TimeUnit.SECONDS));

        assertTrue(client.getSignalProvider().getConnectionState() == ConnectionState.AUTHENTICATED);
        assertEquals(1, connectionChangedObserver.connectionChangedEvents);
        assertTrue(connectionChangedObserver.connected);

        assertTrue(client.disconnect().await(4, TimeUnit.SECONDS));

        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testConnectSignalConnectTwoTimesQuicklyReturnsSameFuture() throws Exception {

        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);

        // null out the executor so my connect requests don't come back.
        ((MockSignalProvider) client.getSignalProvider()).executor = new NullExecutor();

        ObservableFuture<ConnectionHandle> future1 = client.connect();
        assertFalse("Future1 can't be done already or my test wont work!", future1.isDone());
        ObservableFuture<ConnectionHandle> future2 = client.connect();
        assertFalse("Future2 can't be done already or my test wont work!", future2.isDone());

        assertTrue(future1 == future2);
    }

    @Test
    public void testConnectBlocksOnSubscriptionComplete() throws Exception {
        apiConnection = new MockApiConnection();
        signalProvider = new MockSignalProvider();
        client = new DefaultZipwhipClient(null, null, apiConnection, signalProvider) {
            @Override
            protected ServerResponse executeSync(String method, Map<String, Object> params) throws Exception {
                ((MockSignalProvider)signalProvider).sendSubscriptionCompleteCommand(new SubscriptionCompleteCommand("", null));
                return new StringServerResponse("{success:true}", true, "{success:true}", null);
            }
        };

        client.setSettingsStore(new MemorySettingStore());

        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);

        client.connect().get(5, TimeUnit.SECONDS);

        assertTrue(client.getSignalProvider().getConnectionState() == ConnectionState.AUTHENTICATED);
        assertEquals(1, connectionChangedObserver.connectionChangedEvents);
        assertTrue(connectionChangedObserver.connected);

        client.disconnect().get(5, TimeUnit.SECONDS);

        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testConnectBlocksOnSubscriptionCompleteWithDelay() throws Exception {
        apiConnection = new MockApiConnection();
        signalProvider = new MockSignalProvider();
        client = new DefaultZipwhipClient(null, null, apiConnection, signalProvider) {
            @Override
            protected ServerResponse executeSync(String method, Map<String, Object> params) throws Exception {
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            ((MockSignalProvider)signalProvider).sendSubscriptionCompleteCommand(new SubscriptionCompleteCommand("", null));
                        } catch (InterruptedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                });

                Thread.sleep(1000);
                return new StringServerResponse("{success:true}", true, "{success:true}", null);
            }
        };

        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] hit = {false};
        client.getSignalProvider().getSubscriptionCompleteReceivedEvent().addObserver(new Observer<SubscriptionCompleteCommand>() {
            @Override
            public void notify(Object sender, SubscriptionCompleteCommand item) {
                hit[0] = true;
                latch.countDown();
            }
        });

        ObservableFuture<ConnectionHandle> future = client.connect();

        TestUtil.awaitAndAssertSuccess(future);

        latch.await(4, TimeUnit.SECONDS);
        assertTrue(hit[0]);

        assertTrue(client.getSignalProvider().getConnectionState() == ConnectionState.AUTHENTICATED);
        assertTrue(client.isConnected());

        assertEquals(1, connectionChangedObserver.connectionChangedEvents);
        assertTrue(connectionChangedObserver.connected);

        client.disconnect().get(5, TimeUnit.SECONDS);

        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testConnectBlocksOnSubscriptionCompleteWithFailure() throws Exception {
        ((ClientZipwhipNetworkSupport) client).signalsConnectTimeoutInSeconds = 1;
        ((MockApiConnection) client.getConnection()).missSignalsConnect = true;
        client.setSettingsStore(new MemorySettingStore());

        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().getConnectionState() == ConnectionState.CONNECTED);

        final boolean[] hit = {false};
        client.getSignalProvider().getSubscriptionCompleteReceivedEvent().addObserver(new Observer<SubscriptionCompleteCommand>() {
            @Override
            public void notify(Object sender, SubscriptionCompleteCommand item) {
                hit[0] = true;
            }
        });

        final CountDownLatch latch = new CountDownLatch(2);
        client.getSignalProvider().getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                if (Boolean.FALSE.equals(item))
                    latch.countDown();
                else
                    latch.countDown();
            }
        });
        ObservableFuture<ConnectionHandle> future = client.connect();

        future.addObserver(new Observer() {
            @Override
            public void notify(Object sender, Object item) {
                Logger.getLogger(DefaultZipwhipClientTest.class).debug("Who called me? " + item);
            }
        });

        future.get(5, TimeUnit.SECONDS);

        // it shouldn't be a success since you didn't get a SubscriptionCompleteCommand.
        assertFalse("Should not have said success", future.isSuccess());

        assertFalse(hit[0]);

        assertTrue("Latch finished?", latch.await(10, TimeUnit.SECONDS));

        // it reconnected succesfully?
        assertTrue("SignalProvider connection should NOT be brought back up",
                client.getSignalProvider().getConnectionState() == ConnectionState.DISCONNECTED);
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testSessionChallenge() throws Exception {
        String result = client.sessionChallenge(MOBILE_NUMBER, null);
        assertNotNull(result);
        assertEquals("ed69db37-c61c-4603-9333-af691d4aaaca", result);
    }

    private class ConnectionChangedObserver implements Observer<Boolean> {

        int connectionChangedEvents;
        boolean connected;

        @Override
        public void notify(Object sender, Boolean item) {
            connectionChangedEvents++;
            connected = item;
        }
    }

    public class MockApiConnection extends DestroyableBase implements ApiConnection {

        public final static String SESSION_CHALLENGE_RESPONSE = "{\"response\":\"ed69db37-c61c-4603-9333-af691d4aaaca\",\"sessions\":null,\"success\":true}";
        public final static String SIGNALS_CONNECT_RESPONSE_SUCCESS = "{\"response\":{\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorResponse\",\"fingerprint\":\"733786486\",\"root\":\"240912110964375552\",\"tokens\":[{\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorToken\",\"contact\":-1,\"device\":132961202,\"fingerprint\":\"733786486\",\"message\":\"240912110964375552\"}]},\"sessions\":null,\"success\":true}";
        public final static String SIGNALS_CONNECT_RESPONSE_FAILURE = "{\"response\":{\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorResponse\",\"fingerprint\":\"733786486\",\"root\":\"240912110964375552\",\"tokens\":[{\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorToken\",\"contact\":-1,\"device\":132961202,\"fingerprint\":\"733786486\",\"message\":\"240912110964375552\"}]},\"sessions\":null,\"success\":false}";

        String sessionKey = "1234-5678-9012-3456:123456";

        boolean failSignalsConnect = false;
        boolean failSignalsConnectWithException = false;
        boolean missSignalsConnect = false;

        @Override
        public ObservableFuture<String> send(String method, Map<String, Object> params) throws Exception {

            ObservableFuture<String> result = new DefaultObservableFuture<String>(this);

            if (ZipwhipNetworkSupport.CHALLENGE_REQUEST.equalsIgnoreCase(method)) {
                result.setSuccess(SESSION_CHALLENGE_RESPONSE);
            }

            if (ZipwhipNetworkSupport.SIGNALS_CONNECT.equalsIgnoreCase(method)) {
                if (failSignalsConnectWithException) {
                    throw new Exception("Faking a signals/connect exception");
                } else if (failSignalsConnect) {
                    result.setSuccess(SIGNALS_CONNECT_RESPONSE_FAILURE);
                } else {
                    result.setSuccess(SIGNALS_CONNECT_RESPONSE_SUCCESS);

                    if (!missSignalsConnect) {
                        ((MockSignalProvider) client.getSignalProvider()).sendSubscriptionCompleteCommand(new SubscriptionCompleteCommand(null, null));
                    }
                }
            }

            return result;
        }

        @Override
        public ObservableFuture<String> send(String method, Map<String, Object> params, List<File> files) throws Exception {
            return null;
        }

        @Override
        public void setSessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
        }

        @Override
        public String getSessionKey() {
            return sessionKey;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void setAuthenticator(SignTool authenticator) {

        }

        @Override
        public SignTool getAuthenticator() {
            return null;
        }

        @Override
        public void setHost(String host) {

        }

        @Override
        public String getHost() {
            return null;
        }

        @Override
        public void setApiVersion(String apiVersion) {

        }

        @Override
        public String getApiVersion() {
            return null;
        }

        @Override
        protected void onDestroy() {

        }

        @Override
        public ObservableFuture<InputStream> sendBinaryResponse(String method, Map<String, Object> params) throws Exception {
            return null;
        }

    }

}
