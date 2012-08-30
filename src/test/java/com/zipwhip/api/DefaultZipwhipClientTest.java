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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
        ((DefaultZipwhipClient) client).setImportantTaskExecutor(new ImportantTaskExecutor());
        client.setSettingsStore(new MemorySettingStore());
    }

    @After
    public void tearDown() {
        client.getSettingsStore().clear();
    }

    @Test
    public void testConnect() throws Exception {

        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().isConnected());

        client.connect();

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

        client.connect();

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

        client.connect();

        assertFalse(client.getSignalProvider().isConnected());
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testDisconnect() throws Exception {
        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();
        client.addSignalsConnectionObserver(connectionChangedObserver);
        assertFalse(client.getSignalProvider().isConnected());

        client.connect();

        assertTrue(client.getSignalProvider().isConnected());
        assertEquals(1, connectionChangedObserver.connectionChangedEvents);
        assertTrue(connectionChangedObserver.connected);

        client.disconnect();

        assertFalse(client.getSignalProvider().isConnected());
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testConnectBlocksOnSubscriptionComplete() throws Exception {
        apiConnection = new MockApiConnection();
        final MockSignalProvider sp = new MockSignalProvider();
        client = new DefaultZipwhipClient(apiConnection, sp){
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

        client.disconnect();

        assertFalse(client.getSignalProvider().isConnected());
        assertEquals(2, connectionChangedObserver.connectionChangedEvents);
        assertFalse(connectionChangedObserver.connected);
    }

    @Test
    public void testConnectBlocksOnSubscriptionCompleteWithDelay() throws Exception {
        apiConnection = new MockApiConnection();
        final MockSignalProvider sp = new MockSignalProvider();
        client = new DefaultZipwhipClient(apiConnection, sp){
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
        client.setSettingsStore(new MemorySettingStore());

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

        client.disconnect();

        assertFalse(client.getSignalProvider().isConnected());
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
