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
    public void testConnect() throws Exception {
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
    }

    @Test
    public void testSubscriptionCompleteNeverComesBack() throws Exception {
        fail();
    }

    private <T> T await(ObservableFuture <T> future) throws ExecutionException, InterruptedException {
        assertTrue(future.await(10, TimeUnit.SECONDS));

        if (future.isFailed()) {
            if (future.getCause() == null) {
                fail("Unknown exception. Future failed.");
            }

            fail(future.getCause().toString());
        }

        return future.get();
    }
}
