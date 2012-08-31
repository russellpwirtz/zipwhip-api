package com.zipwhip.api;

import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.response.StringServerResponse;
import com.zipwhip.api.settings.MemorySettingStore;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
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

    ZipwhipClient client;
    ApiConnection apiConnection;

    public final static String MOBILE_NUMBER = "2069797502";

    @Before
    public void setUp() throws Exception {
        apiConnection = new MockApiConnection();
        client = new DefaultZipwhipClient(apiConnection, new MockSignalProvider());
        ((DefaultZipwhipClient) client).signalsConnectTimeoutInSeconds = 1;
        ((DefaultZipwhipClient) client).setImportantTaskExecutor(new ImportantTaskExecutor());
        client.setSettingsStore(new MemorySettingStore());
    }

    @After
    public void tearDown() {
        client.getSettingsStore().clear();
    }

    @Test
    public void testDisconnectConnectDisconnect() throws Exception{

        ObservableFuture<Boolean> connectFuture1 = client.connect();
        assertTrue(connectFuture1.get());
        assertTrue(connectFuture1.isSuccess());

        ObservableFuture<Void> disconnectFuture1 = client.disconnect();
        disconnectFuture1.await();
        assertTrue(disconnectFuture1.isSuccess());

        ObservableFuture<Boolean> connectFuture2 = client.connect();
        assertFalse(connectFuture1 == connectFuture2);
        assertTrue(connectFuture2.get());
        assertTrue(connectFuture2.isSuccess());

        ObservableFuture<Void> disconnectFuture2 = client.disconnect();
        assertFalse(disconnectFuture1 == disconnectFuture2);
        disconnectFuture2.await();
        assertTrue(disconnectFuture2.isSuccess());
    }

    // connect
    // callback
    // latch
    // close channel (close from bottom up)
    // result: all client events fire in client executor
    // result:

    @Test
    public void testConnectDisconnectNoDeadlock() throws Exception {

        class ConnectOrDisconnectBasedOnIterationNumberRunnable implements Runnable {

            int iteration;

            public ConnectOrDisconnectBasedOnIterationNumberRunnable(Integer iteration) {
                this.iteration = iteration;
            }

            @Override
            public void run() {
                if (iteration % 2 == 0) {
                    try {
                        System.out.println("Connecting at iteration " + iteration);
                        ObservableFuture<Boolean> connectFuture = client.connect();
                        connectFuture.await();
                    } catch (Exception e) {
                        fail("Failed to connect! " + e.getMessage());
                    }
                } else {
                    try {
                        System.out.println("Disconnecting at iteration " + iteration);
                        ObservableFuture<Void> disconnectFuture = client.disconnect();
                        disconnectFuture.await();
                    } catch (Exception e) {
                        fail("Failed to disconnect! " + e.getMessage());
                    }
                }
            }
        }

        ExecutorService executor = Executors.newCachedThreadPool();

        for (int i = 0; i < 100; i++) {
            executor.execute(new ConnectOrDisconnectBasedOnIterationNumberRunnable(i));
        }

        executor.shutdown();
    }


    @Test
    public void testConnect() throws Exception {
        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().isConnected());

        ObservableFuture<Boolean> future = client.connect();
        assertTrue(future.await(5, TimeUnit.SECONDS));

        assertTrue(future.isDone());
        assertTrue(future.isSuccess());

        assertTrue(client.getSignalProvider().isConnected());
        assertEquals(1, connectionChangedObserver.connectionChangedEvents);
        assertTrue(connectionChangedObserver.connected);
    }

    @Test
    public void testConnectSignalConnectFailure() throws Exception {

        ((MockApiConnection) apiConnection).failSignalsConnect = true;

        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().isConnected());

        final CountDownLatch latch = new CountDownLatch(1);
        client.getSignalProvider().onConnectionChanged(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                if (Boolean.FALSE.equals(item))
                    latch.countDown();
            }
        });

        ObservableFuture<Boolean> future = client.connect();

        assertTrue("Didn't finish!?", future.await(5, TimeUnit.SECONDS));

        assertTrue(future.isDone());
        assertFalse(future.isSuccess());

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertFalse(client.getSignalProvider().isConnected());
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testConnectSignalConnectException() throws Exception {

        ((MockApiConnection) apiConnection).failSignalsConnectWithException = true;

        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().isConnected());

        final CountDownLatch latch = new CountDownLatch(1);
        client.getSignalProvider().onConnectionChanged(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                if (item.equals(Boolean.FALSE))
                    latch.countDown();
            }
        });

        client.connect().get(5, TimeUnit.SECONDS);

        latch.await(5, TimeUnit.SECONDS);

        assertFalse(client.getSignalProvider().isConnected());
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testDisconnect() throws Exception {
        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().isConnected());

        assertTrue(client.connect().await(4, TimeUnit.SECONDS));

        assertTrue(client.getSignalProvider().isConnected());
        assertEquals(1, connectionChangedObserver.connectionChangedEvents);
        assertTrue(connectionChangedObserver.connected);

        assertTrue(client.disconnect().await(4, TimeUnit.SECONDS));

        assertFalse(client.getSignalProvider().isConnected());
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testConnectSignalConnectTwoTimesQuicklyReturnsSameFuture() throws Exception {

        assertFalse(client.getSignalProvider().isConnected());

        ObservableFuture<Boolean> future1 = client.connect();
        ObservableFuture<Boolean> future2 = client.connect();

        assertTrue(future1 == future2);
    }

    @Test
    public void testConnectBlocksOnSubscriptionComplete() throws Exception {
        apiConnection = new MockApiConnection();
        final MockSignalProvider sp = new MockSignalProvider();
        client = new DefaultZipwhipClient(apiConnection, sp) {
            @Override
            protected ServerResponse executeSync(String method, Map<String, Object> params) throws Exception {
                sp.sendSubscriptionCompleteCommand(new SubscriptionCompleteCommand("", null));
                return new StringServerResponse("{success:true}", true, "{success:true}", null);
            }
        };

        ((DefaultZipwhipClient) client).setImportantTaskExecutor(new ImportantTaskExecutor());
        client.setSettingsStore(new MemorySettingStore());

        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().isConnected());

        client.connect().get(5, TimeUnit.SECONDS);

        assertTrue(client.getSignalProvider().isConnected());
        assertEquals(1, connectionChangedObserver.connectionChangedEvents);
        assertTrue(connectionChangedObserver.connected);

        client.disconnect().get(5, TimeUnit.SECONDS);

        assertFalse(client.getSignalProvider().isConnected());
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testConnectBlocksOnSubscriptionCompleteWithDelay() throws Exception {
        apiConnection = new MockApiConnection();
        final MockSignalProvider sp = new MockSignalProvider();
        client = new DefaultZipwhipClient(apiConnection, sp) {
            @Override
            protected ServerResponse executeSync(String method, Map<String, Object> params) throws Exception {
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            sp.sendSubscriptionCompleteCommand(new SubscriptionCompleteCommand("", null));
                        } catch (InterruptedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                });

                Thread.sleep(1000);
                return new StringServerResponse("{success:true}", true, "{success:true}", null);
            }
        };

        ((DefaultZipwhipClient) client).setImportantTaskExecutor(new ImportantTaskExecutor());

        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().isConnected());

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] hit = {false};
        client.getSignalProvider().onSubscriptionComplete(new Observer<SubscriptionCompleteCommand>() {
            @Override
            public void notify(Object sender, SubscriptionCompleteCommand item) {
                hit[0] = true;
                latch.countDown();
            }
        });

        ObservableFuture<Boolean> future = client.connect();

        if (!future.await(5, TimeUnit.SECONDS)) {
            fail("Didnt complete");
        }

        latch.await(4, TimeUnit.SECONDS);
        assertTrue(hit[0]);


        assertTrue(future.isDone());
        assertTrue(future.isSuccess());

        assertTrue(client.getSignalProvider().isConnected());
        assertEquals(1, connectionChangedObserver.connectionChangedEvents);
        assertTrue(connectionChangedObserver.connected);

        client.disconnect().get(5, TimeUnit.SECONDS);

        assertFalse(client.getSignalProvider().isConnected());
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testConnectBlocksOnSubscriptionCompleteWithFailure() throws Exception {
        ((ClientZipwhipNetworkSupport) client).signalsConnectTimeoutInSeconds = 1;
        ((DefaultZipwhipClient) client).setImportantTaskExecutor(new ImportantTaskExecutor());
        ((MockApiConnection)client.getConnection()).missSignalsConnect = true;
        client.setSettingsStore(new MemorySettingStore());

        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().isConnected());

        final boolean[] hit = {false};
        client.getSignalProvider().onSubscriptionComplete(new Observer<SubscriptionCompleteCommand>() {
            @Override
            public void notify(Object sender, SubscriptionCompleteCommand item) {
                hit[0] = true;
            }
        });

        ObservableFuture<Boolean> future = client.connect();

        future.addObserver(new Observer<ObservableFuture<Boolean>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Boolean> item) {
                Logger.getLogger(DefaultZipwhipClientTest.class).debug("Who called me?");
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        client.getSignalProvider().onConnectionChanged(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                if (Boolean.FALSE.equals(item))
                    latch.countDown();
            }
        });

        if (!future.await(500, TimeUnit.SECONDS)) {
            fail("Didnt complete");
        }

        // it shouldn't be a success since you didn't get a SubscriptionCompleteCommand.
        assertFalse("Should not have said success", future.isSuccess());

        assertFalse(hit[0]);

        latch.await(4, TimeUnit.SECONDS);

        assertFalse("SignalProvider connection should be torn down", client.getSignalProvider().isConnected());
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
