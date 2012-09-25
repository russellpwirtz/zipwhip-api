package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.CommonExecutorFactory;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.CommonExecutorTypes;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.api.signals.sockets.SocketSignalProvider;
import com.zipwhip.api.signals.sockets.netty.pipeline.TestRawSocketIoChannelPipelineFactory;
import com.zipwhip.concurrent.ConfiguredFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.concurrent.TestUtil;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.util.Factory;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
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

    private static final Logger LOGGER = Logger.getLogger(NettySignalConnectionTest.class);

    NettySignalConnection connection;

    @Before
    public void setUp() throws Exception {
        connection = new NettySignalConnection();
    }

    @Test
    public void testConnectAndDisconnectWithNull() throws Exception {

        ConnectionHandle connectionHandle = TestUtil.connect(connection);

        assertNotNull(connection.getConnectionHandle());

        ConnectionHandle connectionHandle1 = TestUtil.awaitAndAssertSuccess(connection.disconnect());

        assertNull(connection.getConnectionHandle());
    }

    @Test
    public void testDestroy() {
        TestUtil.awaitAndAssertSuccess(TestUtil.connect(connection).disconnect());
        connection.destroy();
    }

    @After
    public void tearDown() throws Exception {
        if (connection == null || connection.isDestroyed()){
            return;
        }

        connection.disconnect().await();
    }

    @Test
    public void testCancelWhileConnecting() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        final CountDownLatch latch3 = new CountDownLatch(1);
        final ObservableFuture[] future = new ObservableFuture[1];

        connection = new NettySignalConnection() {
            @Override
            protected void executeConnect(ConnectionHandle connectionHandle, SocketAddress address) throws Throwable {
                future[0].cancel();

                super.executeConnect(connectionHandle, address);
            }

            @Override
            protected void executeDisconnectDestroyConnection(ConnectionHandle connectionHandle, boolean causedByNetwork) {
                super.executeDisconnectDestroyConnection(connectionHandle, causedByNetwork);

                latch3.countDown();
            }
        };

        future[0] = connection.connect();

        latch2.countDown();
        latch3.await();

        assertTrue(connection.getConnectionState() == ConnectionState.DISCONNECTED);

    }

    @Test
    public void testCancelWhileConnectingFuture1() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        NettySignalConnection connection = new NettySignalConnection() {
            @Override
            protected void executeConnect(ConnectionHandle connectionHandle, SocketAddress address) throws Throwable {
                Thread.sleep(1000);
                super.executeConnect(connectionHandle, address);
            }
        };

        ObservableFuture<ConnectionHandle> future1 = connection.connect();

        ObservableFuture<ConnectionHandle> future2 = connection.disconnect(true);

        latch.countDown();

        TestUtil.awaitAndAssertSuccess(future2);

        future1.await();

        assertTrue("Future1 should be cancelled", future1.isCancelled());
        assertTrue(connection.getConnectionState() == ConnectionState.DISCONNECTED);
        assertTrue(connection.getConnectionHandle() == null);
        assertNull(connection.connectFuture);
        assertNull(connection.disconnectFuture);
        assertTrue("ReconnectStrategy must be bound!", connection.getReconnectStrategy().isStarted());
    }

    @Test
    public void testCancelWhileConnectingFuture2() throws Exception {
        NettySignalConnection connection = new NettySignalConnection();

        final CountDownLatch latch = new CountDownLatch(1);
        connection.getDisconnectEvent().addObserver(new Observer<ConnectionHandle>() {
            @Override
            public void notify(Object sender, ConnectionHandle item) {
                try {
                    LOGGER.debug("hit the latch!");
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        ObservableFuture<ConnectionHandle> future1 = connection.connect();
        TestUtil.awaitAndAssertSuccess(future1);

        ObservableFuture<ConnectionHandle> future2 = connection.disconnect(true);
        ObservableFuture<ConnectionHandle> future3 = connection.connect();

        latch.countDown();

        TestUtil.awaitAndAssertSuccess(future3);

//        future2.await();

        assertTrue(future2.isCancelled());
        assertTrue("State should be connected", connection.getConnectionState() == ConnectionState.CONNECTED);
        assertNotNull("Connection should exist", connection.getConnectionHandle());
        assertNull(connection.connectFuture);
        assertNull(connection.disconnectFuture);
        assertTrue("ReconnectStrategy must be bound!", connection.getReconnectStrategy().isStarted());
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

        assertTrue(connection.getConnectionState() == ConnectionState.CONNECTED);
        assertFalse(connection.connectionHandle.isDestroyed());
        assertTrue(((ChannelWrapperConnectionHandle) connection.connectionHandle).channelWrapper.channel.isConnected());
        ((ChannelWrapperConnectionHandle)connection.connectionHandle).channelWrapper.channel.close().await();

        latch.await(50, TimeUnit.SECONDS);
        assertFalse(connection.getConnectionState() == ConnectionState.CONNECTED);
        assertNull("After a close, the wrapper should be null", connection.connectionHandle);
        assertTrue(disconnectCalled[0]);
    }

    @Test
    public void testBadPort() throws Exception {

        connection.setConnectTimeoutSeconds(1);
        connection.setAddress(new InetSocketAddress(((InetSocketAddress)connection.getAddress()).getAddress(), 23423));

        assertNull("Expected connection to be connected", connection.connect().get(300, TimeUnit.SECONDS));
    }


    @Test
    public void testGoodPort() throws Exception {
        TestUtil.awaitAndAssertSuccess(connection.connect());
    }

    @Test
    public void testConnectDisconnectCycle() throws Exception {
        assertFalse("Expected connection to be connected", connection.getConnectionState() == ConnectionState.CONNECTED);
        assertFalse("Expected connection to be connected", connection.connect().get().isDestroyed());
        assertTrue("Expected connection to be connected", connection.getConnectionState() == ConnectionState.CONNECTED);

        ConnectionHandle con = connection.disconnect().get();

        assertNotNull("Expected connection to be connected", con);
        assertTrue("Expected connection to be connected", con.isDestroyed());
        assertFalse("Expected connection to be connected", connection.getConnectionState() == ConnectionState.CONNECTED);
    }

    /**
     * Keep this test, it finds deadlocks
     *
     * @throws Exception
     */
    @Test
    public void testMultipleConnectDisconnects() throws Exception {
        for (int i = 0; i < 100; i++) {
            assertFalse("Expected connection to be connected", connection.getConnectionState() == ConnectionState.CONNECTED);
            ObservableFuture<ConnectionHandle> future = connection.connect();
            future.await();

            assertTrue(future.isSuccess());
            assertFalse("Expected connection to be connected", future.getResult().isDestroyed());
            assertTrue("Expected connection to be connected", connection.getConnectionState() == ConnectionState.CONNECTED);

            assertNotNull("Expected connection to be connected", connection.disconnect().get());
            assertFalse("Expected connection to be connected", connection.getConnectionState() == ConnectionState.CONNECTED);
        }
    }

    @Test
    public void testSimpleConnectDisconnect() throws Exception {

        ConnectionHandle connectionHandle = TestUtil.awaitAndAssertSuccess(connection.connect());
        assertNotNull(connectionHandle);
        assertFalse(connectionHandle.isDestroyed());
        assertTrue(connection.getConnectionState() == ConnectionState.CONNECTED);

        ConnectionHandle connectionHandle2 = TestUtil.awaitAndAssertSuccess(connection.disconnect());

        assertSame(connectionHandle, connectionHandle2);
    }

    @Test
    public void testReconnectStrategy() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        connection.setReconnectStrategy(new ReconnectStrategy() {
            @Override
            protected void doStrategyWithoutBlocking() {
                try {
                    LOGGER.debug("Doing await on latch2");
                    latch2.await();
                    LOGGER.debug("latch2 cleared. doing connect unblocking");

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
                LOGGER.debug("Hit connectEvent: " + item);
                LOGGER.debug("Hit connectEvent: " + connection);
                if (connection.getConnectionState() == ConnectionState.CONNECTED) {
                    latch.countDown();
                    LOGGER.debug("After onConnect event " + (connection.getConnectionState() == ConnectionState.CONNECTED));
                }
            }
        });

        ConnectionHandle connectionHandle = TestUtil.awaitAndAssertSuccess(connection.disconnect(true));
        assertNotNull(connectionHandle);
        assertTrue(connectionHandle.isDestroyed());
        assertNull(connection.getConnectionHandle());
        assertEquals(connection.getConnectionState(), ConnectionState.DISCONNECTED);
        assertTrue(connection.getReconnectStrategy().isStarted());
        assertFalse(connection.getReconnectStrategy().isDestroyed());

        LOGGER.debug("Doing count down");
        latch2.countDown();

        // reconnecting should happen here.

        // assume a connect is happening
        latch.await(60, TimeUnit.SECONDS);

        System.out.println(connection.getConnectionState() == ConnectionState.CONNECTED);
        assertTrue("IsConnected", connection.getConnectionState() == ConnectionState.CONNECTED);
    }

    @Test
    public void testPongTimeoutCausesReconnect() throws Exception{

        CountDownLatch latch = new CountDownLatch(1);

        ReconnectRightAwayReconnectStrategy reconnectStrategy = new ReconnectRightAwayReconnectStrategy(latch);
        TestRawSocketIoChannelPipelineFactory pipelineFactory = new TestRawSocketIoChannelPipelineFactory(1, 1);

        connection = new NettySignalConnection(reconnectStrategy, pipelineFactory);

        ConnectObserver connectObserver = new ConnectObserver();
        connection.getConnectEvent().addObserver(connectObserver);

        DisconnectObserver disconnectObserver = new DisconnectObserver();
        connection.getDisconnectEvent().addObserver(disconnectObserver);

        Future<ConnectionHandle> connectFuture = connection.connect();
        connectFuture.get();

        Future<Boolean> sendFuture = connection.send(new ConnectCommand(StringUtil.EMPTY_STRING));
        sendFuture.get();

        System.out.println("Connection isConnected returns " + (connection.getConnectionState() == ConnectionState.CONNECTED));
        assertTrue(connection.getConnectionState() == ConnectionState.CONNECTED);
        assertEquals(1, connectObserver.connectCount);

        latch.await(50, TimeUnit.SECONDS);
        assertFalse(connection.getConnectionState() == ConnectionState.CONNECTED);
        assertEquals(1, disconnectObserver.disconnectCount);
        assertEquals(1, reconnectStrategy.reconnectCount);

        System.out.println("DONE:testPongTimeoutCausesReconnect");
    }

    @Test
    public void testPongTimeoutCausesReconnect2() throws Exception{
        connection.destroy();
        connection = new NettySignalConnection(new CommonExecutorFactory() {
            @Override
            public ExecutorService create(CommonExecutorTypes type, String name) {
                return SimpleExecutor.getInstance();
            }
        }, null, null);


        final CountDownLatch latch = new CountDownLatch(2);

        ReconnectRightAwayReconnectStrategy reconnectStrategy = new ReconnectRightAwayReconnectStrategy(latch);
        TestRawSocketIoChannelPipelineFactory pipelineFactory = new TestRawSocketIoChannelPipelineFactory(1, 1);
        ConnectionChangedObserver connectionChangedObserver = new ConnectionChangedObserver();

        connection = new NettySignalConnection(reconnectStrategy, pipelineFactory);
        SocketSignalProvider signalProvider = new SocketSignalProvider(connection);

        signalProvider.getConnectionChangedEvent().addObserver(connectionChangedObserver);

        ConnectionHandle connectionHandle = TestUtil.awaitAndAssertSuccess(signalProvider.connect());
        assertFalse(connectionHandle.isDestroyed());

        assertTrue(signalProvider.getConnectionState().toString(), signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);
        connectionChangedObserver.latch.await();
        assertEquals(1,connectionChangedObserver.hitCount);

        signalProvider.getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                if (item == true) {
                    latch.countDown();
                }
            }
        });

        assertTrue(latch.await(50, TimeUnit.SECONDS));
        assertTrue(connection.getConnectionState().toString(), connection.getConnectionState() == ConnectionState.CONNECTED);
        assertTrue(signalProvider.getConnectionState().toString(), signalProvider.getConnectionState() == ConnectionState.AUTHENTICATED);

        assertEquals(3, connectionChangedObserver.hitCount);

        System.out.println("DONE:testPongTimeoutCausesReconnect");
    }

    private class ConnectionChangedObserver implements Observer<Boolean> {
        CountDownLatch latch = new CountDownLatch(1);

        int hitCount = 0;

        @Override
        public void notify(Object sender, Boolean item) {
            hitCount ++;
            latch.countDown();
        }
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
            connection.connect();
            latch.countDown();
        }

        @Override
        protected void onDestroy() {

        }
    }

    private class ConnectObserver implements Observer<ConnectionHandle> {

        int connectCount = 0;

        @Override
        public void notify(Object sender, ConnectionHandle item) {
            connectCount++;
        }
    }

    private class DisconnectObserver implements Observer<ConnectionHandle> {

        int disconnectCount = 0;

        @Override
        public void notify(Object sender, ConnectionHandle item) {
            disconnectCount++;
        }
    }

}
