package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: msmyers
 * Date: 9/4/12
 * Time: 8:25 PM
 * <p/>
 * Tests for NettyConnection.
 */

public class NettyConnectionTests extends MockConnectionTests {


    @Before
    public void setUp() throws Exception {
        ReconnectStrategy reconnectStrategy = new DefaultReconnectStrategy();
        RawSocketIoChannelPipelineFactory channelPipelineFactory = new RawSocketIoChannelPipelineFactory(30, 30);
        this.signalConnection = new NettySignalConnection(reconnectStrategy, channelPipelineFactory);
    }

    @Test
    public void testConnectedEventBeforeConnectFutureFinish() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch waitLatch = new CountDownLatch(1);
        final CountDownLatch waitLatch2 = new CountDownLatch(1);

        final boolean[] waiting = {false};
        signalConnection.getConnectEvent().addObserver(new Observer<ConnectionHandle>() {
            @Override
            public void notify(Object sender, ConnectionHandle item) {
                latch.countDown();
                waiting[0] = true;
                waitLatch2.countDown();

                try {
                    waitLatch.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        signalConnection.getDisconnectEvent().addObserver(new AssertDisconnectedStateObserver(false));

        ObservableFuture<ConnectionHandle> future = signalConnection.connect();

        latch.await();
        waitLatch2.await();
        assertTrue(waiting[0]);

        // future is blocked waiting for connectEvent to finish?
        assertFalse("Future was not done", future.isDone());

        waitLatch.countDown();
        assertTrue("Future finished", future.await(2, TimeUnit.SECONDS));

        assertSuccess(future);
    }

    @Test
    public void testDisconnectAbruptlyViaDisconnect() throws Exception {
        ConnectionHandle connectionHandle = connect();

        signalConnection.getDisconnectEvent().addObserver(new AssertDisconnectedStateObserver(true));
        connectionHandle.getDisconnectFuture().addObserver(new AssertDisconnectedStateFutureObserver(true));

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        signalConnection.getDisconnectEvent().addObserver(new Observer<ConnectionHandle>() {
            @Override
            public void notify(Object sender, ConnectionHandle item) {
                latch1.countDown();
            }
        });
        connectionHandle.getDisconnectFuture().addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                latch2.countDown();
            }
        });

        ((ChannelWrapperConnectionHandle) connectionHandle).channelWrapper.channel.disconnect().await();

        latch1.await();
        latch2.await();

        assertTrue("DisconnectedViaNetwork is wrong", connectionHandle.disconnectedViaNetwork());
        assertNull(signalConnection.getConnectionHandle());
    }

}
