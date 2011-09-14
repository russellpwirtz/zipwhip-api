package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.JsonSignal;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.events.Observer;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/30/11
 * Time: 3:30 PM
 */
public class SocketSignalProviderTest {

    private SignalProvider provider;
    private SignalConnection connection;

    @Before
    public void setUp() throws Exception {
        connection = new MockSignalConnection();
        provider = new SocketSignalProvider(connection);
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
                System.out.println("testConnect - provider.onConnectionChanged " + item);
                Assert.assertTrue(item);
            }
        });

        Future<Boolean> future = provider.connect();

        Assert.assertNotNull(future);
        Assert.assertTrue(future.get());
    }

    @Test
    public void testDisconnect() throws Exception {

        Future<Boolean> future = provider.connect();

        Assert.assertNotNull(future);
        Assert.assertTrue(future.get());

        provider.onConnectionChanged(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("testDisconnect - provider.onConnectionChanged " + item);
                Assert.assertFalse(item);
            }
        });

        Future<Void> task = provider.disconnect();
        task.get();
    }

    @Test
    public void testOnSignalReceived() throws Exception {

        provider.onSignalReceived(new Observer<List<com.zipwhip.api.signals.Signal>>() {
            @Override
            public void notify(Object sender, List<com.zipwhip.api.signals.Signal> item) {
                System.out.println("testOnSignalReceived - provider.onSignalReceived " + item);
                Assert.assertNotNull(item);
                Assert.assertTrue(item.get(0) instanceof JsonSignal);
            }
        });

        connection.send(null);
    }

    @Test
    public void testOnConnectionChanged() throws Exception {

        provider.onConnectionChanged(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("testOnConnectionChanged - provider.onConnectionChanged CONNECT " + item);
                Assert.assertFalse(item);
            }
        });

        Future<Boolean> future = provider.connect();

        Assert.assertNotNull(future);
        Assert.assertTrue(future.get());

        provider.onConnectionChanged(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("testOnConnectionChanged - provider.onConnectionChanged DISCONNECT " + item);
                Assert.assertFalse(item);
            }
        });

        Future<Void> task = provider.disconnect();
        task.get();
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
