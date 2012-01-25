package com.zipwhip.api;

import com.zipwhip.api.signals.sockets.SocketSignalProvider;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.SignTool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/18/11
 * Time: 11:11 AM
 */
public class DefaultZipwhipClientTest {

    ZipwhipClient client;

    public final static String MOBILE_NUMBER = "2069797502";

    public final static String SESSION_CHALLENGE_RESPONSE = "{\"response\":\"ed69db37-c61c-4603-9333-af691d4aaaca\",\"sessions\":null,\"success\":true}";

    @Before
    public void setUp() throws Exception {
        client = new DefaultZipwhipClient(new MockApiConnection(), new SocketSignalProvider());
    }

    @Test
    public void testConnect() throws Exception {

    }

    @Test
    public void testDisconnect() throws Exception {

    }

    @Test
    public void testSendMessage() throws Exception {

    }

    @Test
    public void testGetMessage() throws Exception {

    }

    @Test
    public void testMessageRead() throws Exception {

    }

    @Test
    public void testMessageDelete() throws Exception {

    }

    @Test
    public void testGetMessageStatus() throws Exception {

    }

    @Test
    public void testGetContact() throws Exception {

    }

    @Test
    public void testGetPresence() throws Exception {

    }

    @Test
    public void testSendSignal() throws Exception {

    }

    @Test
    public void testAddMember() throws Exception {

    }

    @Test
    public void testCarbonEnable() throws Exception {

    }

    @Test
    public void testCarbonEnabled() throws Exception {

    }

    @Test
    public void testSessionChallenge() throws Exception {
        String result = client.sessionChallenge(MOBILE_NUMBER, null);
        Assert.assertNotNull(result);
        Assert.assertEquals("ed69db37-c61c-4603-9333-af691d4aaaca", result);
    }

    @Test
    public void testSessionChallengeConfirm() throws Exception {

    }

    @Test
    public void testSaveContact() throws Exception {

    }

    @Test
    public void testSaveGroup() throws Exception {

    }

    @Test
    public void testSaveUser() throws Exception {

    }

    public class MockApiConnection extends DestroyableBase implements ApiConnection {

        @Override
        public ObservableFuture<String> send(String method, Map<String, Object> params) throws Exception {

            ObservableFuture<String> result = new DefaultObservableFuture<String>(this);

            if (ZipwhipNetworkSupport.CHALLENGE_REQUEST.equalsIgnoreCase(method)) {
                result.setSuccess(SESSION_CHALLENGE_RESPONSE);
                return result;
            }

            return result;
        }

        @Override
        public void setSessionKey(String sessionKey) {

        }

        @Override
        public String getSessionKey() {
            return "";
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
