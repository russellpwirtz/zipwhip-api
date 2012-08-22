package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.events.Observer;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/21/12
 * Time: 4:39 PM
 */
public class NettySignalConnectionTest {

    NettySignalConnection connection;

    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();

        connection = new NettySignalConnection();
    }

    @Test
    public void testBadPort() throws Exception {

        connection.setPort(3123);

        assertFalse("Expected connection to be connected", connection.connect().get());
    }

    @Test
    public void testGoodPort() throws Exception {
        assertTrue("Expected connection to be connected", connection.connect().get());
    }

    @Test
    public void testConnectDisconnectCycle() throws Exception {
        assertFalse("Expected connection to be connected", connection.isConnected());
        assertTrue("Expected connection to be connected", connection.connect().get());
        assertTrue("Expected connection to be connected", connection.isConnected());

        assertNull("Expected connection to be connected", connection.disconnect().get());
        assertFalse("Expected connection to be connected", connection.isConnected());
    }

    /**
     * Keep this test, it finds deadlocks
     *
     * @throws Exception
     */
    @Test
    public void testMultipleConnectDisconnects() throws Exception {
        for (int i = 0; i < 100; i++) {
            assertFalse("Expected connection to be connected", connection.isConnected());
            assertTrue("Expected connection to be connected", connection.connect().get());
            assertTrue("Expected connection to be connected", connection.isConnected());

            assertNull("Expected connection to be connected", connection.disconnect().get());
            assertFalse("Expected connection to be connected", connection.isConnected());
        }
    }

    @Test
    public void testReconnectStrategy() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);


        connection.setReconnectStrategy(new ReconnectStrategy() {
            @Override
            protected void doStrategyWithoutBlocking() {
                try {
                    // can't block
                    connection.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Exception on reconnect");
                }
            }

            @Override
            protected void onDestroy() {

            }
        });

        connection.connect().get();

        connection.onConnect(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                if (connection.isConnected()) {
                    latch.countDown();
                    System.out.println("After onConnect event " + connection.isConnected());
                }
            }
        });

        connection.disconnect(true).get();

        // assume a connect is happening
        latch.await(5, TimeUnit.SECONDS);

        System.out.println(connection.isConnected());
        assert connection.isConnected();
    }
}
