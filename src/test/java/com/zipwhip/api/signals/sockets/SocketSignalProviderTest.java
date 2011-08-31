package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.events.Observer;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/30/11
 * Time: 3:30 PM
 */
public class SocketSignalProviderTest {

    private SignalProvider provider;

    @Before
    public void setUp() throws Exception {
        provider = new SocketSignalProvider(new MockSignalConnection());
    }

    @Test
    public void testIsConnected() throws Exception {
        Assert.assertFalse(provider.isConnected());
    }

    @Test
    public void testConnect() throws Exception {

        provider.onConnectionChanged(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("provider.onConnectionChanged " + item);
                Assert.assertTrue(item);
            }
        });

        Future<Boolean> future = provider.connect();

        Assert.assertNotNull(future);
        Assert.assertTrue(future.get());
    }

    @Test
    public void testDisconnect() throws Exception {

    }

    @Test
    public void testOnSignalReceived() throws Exception {

    }

    @Test
    public void testOnConnectionChanged() throws Exception {

    }

    @Test
    public void testOnNewClientIdReceived() throws Exception {

    }

    @Test
    public void testOnSubscriptionComplete() throws Exception {

    }

    @Test
    public void testOnPresenceReceived() throws Exception {

    }

    @Test
    public void testOnSignalVerificationReceived() throws Exception {

    }

    @Test
    public void testOnVersionChanged() throws Exception {

    }

}
