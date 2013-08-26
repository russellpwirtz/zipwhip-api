package com.zipwhip.api.signals;

import com.ning.http.client.AsyncHttpClient;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.signals.presence.UserAgent;
import com.zipwhip.util.StringUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Date: 8/7/13
 * Time: 3:11 PM
 *
 * @author Michael
 * @version 1
 */
public class SignalProviderTest {

    SignalProviderImpl signalProvider;
    NingSignalsSubscribeActor actor = new NingSignalsSubscribeActor();

    @Before
    public void setUp() throws Exception {
        signalProvider = new SignalProviderImpl();
        signalProvider.setSignalsSubscribeActor(actor);

        actor.setUrl("http://localhost:8080/signal/subscribe");
        actor.setClient(new AsyncHttpClient());
    }

    @Test
    public void testDisconnectWhileAlreadyDisconnected() {
        assertFalse(signalProvider.isDestroyed());
        assertFalse(signalProvider.isConnected());
        assertTrue(signalProvider.disconnect().isDone());
        assertTrue(signalProvider.disconnect().isSuccess());
    }

    @Test
    public void testConnect() throws Exception {
        final String sessionKey = "sessionKey";
        final String subscriptionId = "subscriptionId";
        final String clientId = "6022c6a0-ce28-4f73-bef5-b3df96898d5f";

        assertTrue(StringUtil.isNullOrEmpty(signalProvider.getClientId()));

        ObservableFuture<Void> future = signalProvider.connect(new UserAgent(), clientId);

        await(future);

        assertTrue(future.isSuccess());

        assertTrue(StringUtil.exists(signalProvider.getClientId()));

        assertNotNull(signalProvider.getUserAgent());

        signalProvider.getSubscribeEvent().addObserver(new Observer<SubscribeResult>() {
            @Override
            public void notify(Object sender, SubscribeResult signalSubscribeResult) {
                assertNotNull(signalSubscribeResult);
                assertFalse(signalSubscribeResult.isFailed());
                assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
                assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
                assertEquals(2, signalSubscribeResult.getChannels().size());
            }
        });

        ObservableFuture<SubscribeResult> future1 = signalProvider.subscribe(sessionKey, subscriptionId);

        SubscribeResult signalSubscribeResult = await(future1);

        assertNotNull(signalSubscribeResult);
        assertFalse(signalSubscribeResult.isFailed());
        assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
        assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
        assertEquals(2, signalSubscribeResult.getChannels().size());

        // we need to wait to consume messages
        Thread.sleep(500000);

    }

    @Test
    public void testSubscriptionCompleteNeverComesBack() throws Exception {
        signalProvider.setSignalsSubscribeActor(new MockSignalSubscribeActor());

        final String sessionKey = "sessionKey";
        final String subscriptionId = "subscriptionId";

        assertTrue(StringUtil.isNullOrEmpty(signalProvider.getClientId()));

        ObservableFuture<Void> future = signalProvider.connect(new UserAgent());

        await(future);

        assertTrue(future.isSuccess());

        assertTrue(StringUtil.exists(signalProvider.getClientId()));

        assertNotNull(signalProvider.getUserAgent());

        signalProvider.getSubscribeEvent().addObserver(new Observer<SubscribeResult>() {
            @Override
            public void notify(Object sender, SubscribeResult signalSubscribeResult) {
                assertNotNull(signalSubscribeResult);
                assertFalse(signalSubscribeResult.isFailed());
                assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
                assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
                assertEquals(3, signalSubscribeResult.getChannels().size());
            }
        });

        ObservableFuture<SubscribeResult> future1 = signalProvider.subscribe(sessionKey, subscriptionId);

        try {
            fail("Should not have succeeded: " + future1.get());
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TimeoutException)) {
                fail("Wrong exception: " + e.getCause());
            }
        }

        assertTrue(future1.isFailed());
    }

    @Test
    public void testSubscribeWhenNotConnected() throws ExecutionException, InterruptedException {
        final String sessionKey = "sessionKey";
        final String subscriptionId = "subscriptionId";

        assertTrue(StringUtil.isNullOrEmpty(signalProvider.getClientId()));
        assertFalse(StringUtil.exists(signalProvider.getClientId()));
        assertNull(signalProvider.getUserAgent());

        signalProvider.getSubscribeEvent().addObserver(new Observer<SubscribeResult>() {
            @Override
            public void notify(Object sender, SubscribeResult signalSubscribeResult) {
                assertNotNull(signalSubscribeResult);
                assertFalse(signalSubscribeResult.isFailed());
                assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
                assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
                assertEquals(3, signalSubscribeResult.getChannels().size());
            }
        });

        ObservableFuture<SubscribeResult> future1 = signalProvider.subscribe(sessionKey, subscriptionId);

        try {
            fail("Should not have succeeded: " + future1.get());
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof IllegalStateException)) {
                fail("Wrong exception: " + e.getCause());
            }
        }

        assertTrue(future1.isFailed());

    }

    @Test
    public void testSubscribeWhenWasConnected() throws ExecutionException, InterruptedException {
        final String sessionKey = "sessionKey";
        final String subscriptionId = "subscriptionId";

        assertTrue(StringUtil.isNullOrEmpty(signalProvider.getClientId()));

        ObservableFuture<Void> future = signalProvider.connect(new UserAgent());

        await(future);

        assertTrue(future.isSuccess());

        assertTrue(StringUtil.exists(signalProvider.getClientId()));

        assertNotNull(signalProvider.getUserAgent());

        signalProvider.getSubscribeEvent().addObserver(new Observer<SubscribeResult>() {
            @Override
            public void notify(Object sender, SubscribeResult signalSubscribeResult) {
                assertNotNull(signalSubscribeResult);
                assertFalse(signalSubscribeResult.isFailed());
                assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
                assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
                assertEquals(3, signalSubscribeResult.getChannels().size());
            }
        });

        ObservableFuture<SubscribeResult> future1 = signalProvider.subscribe(sessionKey, subscriptionId);

        SubscribeResult signalSubscribeResult = await(future1);

        assertNotNull(signalSubscribeResult);
        assertFalse(signalSubscribeResult.isFailed());
        assertEquals(signalSubscribeResult.getSessionKey(), sessionKey);
        assertEquals(signalSubscribeResult.getSubscriptionId(), subscriptionId);
        assertEquals(3, signalSubscribeResult.getChannels().size());

        signalProvider.disconnect().await();

        ObservableFuture<SubscribeResult> future2 = signalProvider.subscribe(sessionKey, subscriptionId);

        try {
            fail("Should not have succeeded: " + future2.get());
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof IllegalStateException)) {
                fail("Wrong exception: " + e.getCause());
            }
        }

        assertTrue(future2.isFailed());
    }

    private <T> T await(ObservableFuture <T> future) throws ExecutionException, InterruptedException {
        assertTrue(future.await(50, TimeUnit.SECONDS));

        if (future.isFailed()) {
            if (future.getCause() == null) {
                fail("Unknown exception. Future failed.");
            }

            fail(future.getCause().toString());
        }

        return future.get();
    }
}
