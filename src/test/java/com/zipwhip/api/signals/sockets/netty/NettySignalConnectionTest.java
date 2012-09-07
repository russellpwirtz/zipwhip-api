package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
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
        connection = new NettySignalConnection();
    }

    /**
     * This test should start to fail when we figure out how to detect that a write is going into the ether because the
     * local network has gone down on the client.
     */
    @Test
    public void testWritingIntoChannelWithNetworkDownThrowsNoException() throws Exception {
        Future<ConnectionHandle> connectFuture = connection.connect();
        assertFalse(connectFuture.get().isDestroyed());
        Future<Boolean> sendFuture = connection.send(PingPongCommand.getShortformInstance());
        assertTrue(sendFuture.get());
    }

    // this was a bug we caught. if the channel was closed abruptly we didn't clean up nicely.
    @Test
    public void testChannelClosedAbruptly() throws Exception {
        connection.setReconnectStrategy(null);

        final CountDownLatch latch = new CountDownLatch(1);
        assertFalse("Connecting", connection.connect().get().isDestroyed());
        final boolean[] disconnectCalled = {false};
        connection.getDisconnectEvent().addObserver(new Observer<ConnectionHandle>() {
            @Override
            public void notify(Object sender, ConnectionHandle item) {
                disconnectCalled[0] = true;
                latch.countDown();
            }
        });

        assertTrue(connection.isConnected());
        assertFalse(connection.connectionHandle.isDestroyed());
        assertTrue(((ChannelWrapperConnectionHandle) connection.connectionHandle).channelWrapper.channel.isConnected());
        ((ChannelWrapperConnectionHandle)connection.connectionHandle).channelWrapper.channel.close().await();

        latch.await(50, TimeUnit.SECONDS);
        assertFalse(connection.isConnected());
        assertNull("After a close, the wrapper should be null", connection.connectionHandle);
        assertTrue(disconnectCalled[0]);
    }

    @Test
    public void testBadPort() throws Exception {

        connection.setConnectTimeoutSeconds(1);
        connection.setAddress(new InetSocketAddress(((InetSocketAddress)connection.getAddress()).getAddress(), 23423));

        assertNull("Expected connection to be connected", connection.connect().get());
    }

    @Test
    public void testGoodPort() throws Exception {
        assertFalse("Expected connection to be connected", connection.connect().get().isDestroyed());
    }

    @Test
    public void testConnectDisconnectCycle() throws Exception {
        assertFalse("Expected connection to be connected", connection.isConnected());
        assertFalse("Expected connection to be connected", connection.connect().get().isDestroyed());
        assertTrue("Expected connection to be connected", connection.isConnected());

        ConnectionHandle con = connection.disconnect().get();

        assertNotNull("Expected connection to be connected", con);
        assertTrue("Expected connection to be connected", con.isDestroyed());
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
            ObservableFuture<ConnectionHandle> future = connection.connect();
            future.await();

            assertTrue(future.isSuccess());
            assertFalse("Expected connection to be connected", future.getResult().isDestroyed());
            assertTrue("Expected connection to be connected", connection.isConnected());

            assertNotNull("Expected connection to be connected", connection.disconnect().get());
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

        assertNull(((NettySignalConnection)connection).connectFuture);

        connection.getConnectEvent().addObserver(new Observer<ConnectionHandle>() {
            @Override
            public void notify(Object sender, ConnectionHandle item) {
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
        assertTrue("IsConnected", connection.isConnected());
    }

//    @Test
//    public void testPongTimeoutCausesReconnect() throws Exception{
//
//        CountDownLatch latch = new CountDownLatch(1);
//
//        ReconnectRightAwayReconnectStrategy reconnectStrategy = new ReconnectRightAwayReconnectStrategy(latch);
//        TestRawSocketIoChannelPipelineFactory pipelineFactory = new TestRawSocketIoChannelPipelineFactory(1, 1);
//
//        connection = new NettySignalConnection(reconnectStrategy, pipelineFactory);
//
//        ConnectObserver connectObserver = new ConnectObserver();
//        connection.onConnect(connectObserver);
//
//        DisconnectObserver disconnectObserver = new DisconnectObserver();
//        connection.onDisconnect(disconnectObserver);
//
//        Future<Boolean> connectFuture = connection.connect();
//        connectFuture.get();
//
//        Future<Boolean> sendFuture = connection.send(new ConnectCommand(StringUtil.EMPTY_STRING));
//        sendFuture.get();
//
//        System.out.println("Connection isConnected returns " + connection.isConnected());
//        assertTrue(connection.isConnected());
//        assertEquals(1, connectObserver.connectCount);
//
//        latch.await(50, TimeUnit.SECONDS);
//        assertFalse(connection.isConnected());
//        assertEquals(1, disconnectObserver.disconnectCount);
//        assertEquals(1, reconnectStrategy.reconnectCount);
//
//        System.out.println("DONE:testPongTimeoutCausesReconnect");
//    }

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
