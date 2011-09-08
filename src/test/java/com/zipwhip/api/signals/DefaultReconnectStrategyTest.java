package com.zipwhip.api.signals;

import com.zipwhip.api.signals.sockets.MockReconnectStrategy;
import com.zipwhip.api.signals.sockets.MockSignalConnection;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.events.Observer;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/8/11
 * Time: 11:47 AM
 */
public class DefaultReconnectStrategyTest {

    ReconnectStrategy strategy;
    SignalConnection connection;

    @Before
    public void setUp() throws Exception {
        connection = new MockSignalConnection();
        strategy = new MockReconnectStrategy(connection);
    }

    @Test
    public void testSetSignalConnection() throws Exception {
        Assert.assertTrue(strategy.getSignalConnection() instanceof MockSignalConnection);
        strategy.setSignalConnection(new NettySignalConnection());
        Assert.assertTrue(strategy.getSignalConnection() instanceof NettySignalConnection);
    }

    @Test
    public void testGetSignalConnection() throws Exception {
        Assert.assertTrue(strategy.getSignalConnection() instanceof MockSignalConnection);
    }

    @Test
    public void testStart() throws Exception {

        strategy.start();
        Assert.assertTrue(strategy.isStarted());

        Future<Boolean> task = connection.connect();
        Assert.assertTrue(task.get());

        connection.onConnect(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("Reconnect fired");
                Assert.assertTrue(item);
            }
        });

        connection.disconnect(true).get();

        strategy.stop();
        Assert.assertFalse(strategy.isStarted());
    }

    @Test
    public void testStop() throws Exception {

        strategy.start();
        Assert.assertTrue(strategy.isStarted());

        Future<Boolean> task = connection.connect();
        Assert.assertTrue(task.get());

        connection.onConnect(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("Reconnect fired");
                Assert.assertTrue(item);
            }
        });

        connection.disconnect(true).get();

        strategy.stop();
        Assert.assertFalse(strategy.isStarted());

        connection = null;
        connection = new MockSignalConnection();

        strategy.setSignalConnection(connection);
        strategy.start();
        Assert.assertTrue(strategy.isStarted());

        strategy.stop();
        Assert.assertFalse(strategy.isStarted());

        task = connection.connect();
        Assert.assertTrue(task.get());

        connection.onConnect(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("Should never fire");
                Assert.assertTrue(item);
            }
        });

        connection.disconnect().get();
    }

    @Test
    public void testIsConnected() throws Exception {
        Assert.assertFalse(strategy.isStarted());
        strategy.start();
        Assert.assertTrue(strategy.isStarted());
        strategy.stop();
        Assert.assertFalse(strategy.isStarted());
    }

}
