package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.netty.pipeline.handler.TestRawSocketIoChannelPipelineFactory;
import com.zipwhip.events.Observer;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
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

    @After
    public void tearDown() throws Exception {
//        if (connection != null) {
//            connection.destroy();
//            connection = null;
//        }
    }

    // this was a bug we caught. if the channel was closed abruptly we didn't clean up nicely.
    @Test
    public void testChannelClosedAbruptly() throws Exception {
        connection.setReconnectStrategy(null);

        final CountDownLatch latch = new CountDownLatch(1);
        assertTrue("Connecting", connection.connect().get());
        final boolean[] disconnectCalled = {false};
        connection.onDisconnect(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                disconnectCalled[0] = true;
                latch.countDown();
            }
        });

        assertTrue(connection.isConnected());
        assertTrue(connection.wrapper.isConnected());
        assertTrue(connection.wrapper.channel.isConnected());
        connection.wrapper.channel.close().await();

        latch.await(50, TimeUnit.SECONDS);
        assertFalse(connection.isConnected());
        assertNull("After a close, the wrapper should be null", connection.wrapper);
        assertTrue(disconnectCalled[0]);
//        Thread.sleep(10000);
//        assertFalse(connection.wrapper.isConnected());
//        assertFalse(connection.wrapper.channel.isConnected());
    }

    @Test
    public void testBadPort() throws Exception {

        connection.setConnectTimeoutSeconds(1);
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
        assertTrue(connection.isConnected());
    }

    @Test
    public void testPongTimeoutCausesReconnect() throws Exception{

        CountDownLatch latch = new CountDownLatch(1);

        ReconnectRightAwayReconnectStrategy reconnectStrategy = new ReconnectRightAwayReconnectStrategy(latch);
        TestRawSocketIoChannelPipelineFactory pipelineFactory = new TestRawSocketIoChannelPipelineFactory(1, 1);

        connection = new NettySignalConnection(reconnectStrategy, pipelineFactory);

        ConnectObserver connectObserver = new ConnectObserver();
        connection.onConnect(connectObserver);

        DisconnectObserver disconnectObserver = new DisconnectObserver();
        connection.onDisconnect(disconnectObserver);

        Future<Boolean> connectFuture = connection.connect();
        connectFuture.get();

        Future<Boolean> sendFuture = connection.send(new ConnectCommand(StringUtil.EMPTY_STRING));
        sendFuture.get();

        System.out.println("Connection isConnected returns " + connection.isConnected());
        assertTrue(connection.isConnected());
        assertEquals(1, connectObserver.connectCount);

        latch.await(50, TimeUnit.SECONDS);
        assertFalse(connection.isConnected());
        assertEquals(1, disconnectObserver.disconnectCount);
        assertEquals(1, reconnectStrategy.reconnectCount);

        System.out.println("DONE:testPongTimeoutCausesReconnect");
    }

    private class ReconnectRightAwayReconnectStrategy extends ReconnectStrategy {

        int reconnectCount = 0;
        CountDownLatch latch;

        private ReconnectRightAwayReconnectStrategy(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        protected void doStrategyWithoutBlocking() {
            System.out.println("doStrategyWithoutBlocking");
            reconnectCount++;
            latch.countDown();
        }

        @Override
        protected void onDestroy() {

        }
    }

    private class ConnectObserver implements Observer<Boolean> {

        int connectCount = 0;

        @Override
        public void notify(Object sender, Boolean item) {
            connectCount++;
        }
    }

    private class DisconnectObserver implements Observer<Boolean> {

        int disconnectCount = 0;

        @Override
        public void notify(Object sender, Boolean item) {
            disconnectCount++;
        }
    }

}
