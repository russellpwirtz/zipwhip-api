package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.FakeObservableFuture;
import com.zipwhip.executors.SimpleExecutor;
import org.junit.Before;
import org.junit.Test;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import static junit.framework.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 8/27/12
 * Time: 2:42 PM
 */
public class SignalConnectionDelegateTest {

    SignalConnectionDelegate delegate;
    SignalConnectionBase signalConnection;
//    Factory<ExecutorService> executorFactory;

    // The delegate does disconnect async so we need this to control timing
    CountDownLatch disconnectLatch;

    @Before
    public void setUp() throws Exception {
//        executorFactory = new Factory<ExecutorService>() {
//            @Override
//            public ExecutorService create() {
//                return SimpleExecutor.getInstance();
//            }
//        };
        signalConnection = new MockSignalConnection(SimpleExecutor.getInstance());
        delegate = new SignalConnectionDelegate(signalConnection);
        disconnectLatch = new CountDownLatch(1);
    }

    @Test
    public void testDisconnectIsDestroyed() throws Exception {

        assertFalse(delegate.isDestroyed());

        delegate.destroy();
        assertTrue(delegate.isDestroyed());

        signalConnection.connect().get();
        assertTrue(signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        // Disconnect fails because we are destroyed
        delegate.disconnectAsyncIfActive(true);
        assertTrue(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
    }

    @Test
    public void testDisconnectNetwork() throws Exception {

        assertFalse(delegate.isDestroyed());

        ConnectionHandle connectionHandle = signalConnection.connect().get();
        assertTrue(signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        delegate.setConnectionHandle(connectionHandle);

        // Network
        delegate.disconnectAsyncIfActive(true);
        disconnectLatch.await();

        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
    }

    @Test
    public void testDisconnectNonNetwork() throws Exception {

        assertFalse(delegate.isDestroyed());

        delegate.setConnectionHandle(signalConnection.connect().get());
        assertTrue(signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        // Non-network
        delegate.disconnectAsyncIfActive(false);
        disconnectLatch.await();

        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
    }

    @Test
    public void testIsConnected() throws Exception {

        delegate.setConnectionHandle(signalConnection.connect().get());

        assertTrue(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
        assertFalse(delegate.isDestroyed());

        delegate.disconnectAsyncIfActive(false);
        disconnectLatch.await();

        assertFalse(signalConnection.getConnectionState() == ConnectionState.CONNECTED);
    }

    @Test
    public void testSend() throws Exception {

        delegate.setConnectionHandle(signalConnection.connect().get());

        assertFalse(delegate.isDestroyed());
        delegate.sendAsyncIfActive(PingPongCommand.getShortformInstance());
        delegate.sendAsyncIfActive(PingPongCommand.getShortformInstance());
        delegate.sendAsyncIfActive(PingPongCommand.getShortformInstance());

        assertEquals(3, ((MockSignalConnection) signalConnection).getSent().size());
    }

    @Test
    public void testReceivePong() throws Exception {

        delegate.setConnectionHandle(signalConnection.connect().get());

        assertFalse(delegate.isDestroyed());
        delegate.receivePong(PingPongCommand.getShortformInstance());
        delegate.receivePong(PingPongCommand.getShortformInstance());
        delegate.receivePong(PingPongCommand.getShortformInstance());

        assertEquals(3, ((MockSignalConnection) signalConnection).getPongs().size());
    }


    private static long id;

    private class MockSignalConnection extends SignalConnectionBase implements SignalConnection {

        // We need these to block for testing
        protected final List<Observer<Command>> receiveEvent = new ArrayList<Observer<Command>>();
        protected final List<Observer<Boolean>> connectEvent = new ArrayList<Observer<Boolean>>();
        protected final List<Observer<Boolean>> disconnectEvent = new ArrayList<Observer<Boolean>>();

        protected final List<Command> sent = new ArrayList<Command>();
        protected final List<PingPongCommand> pongs = new ArrayList<PingPongCommand>();

        protected boolean isConnected = false;

        public MockSignalConnection(Executor executor) {
            super(executor);
        }

        public MockSignalConnection() {
            super(SimpleExecutor.getInstance());
        }

        protected void receivePong(ConnectionHandle connectionHandle, PingPongCommand command) {
            pongs.add(command);
        }

        public final List<Command> getSent() {
            return sent;
        }

        public final List<PingPongCommand> getPongs() {
            return pongs;
        }

        @Override
        protected void executeDisconnectDestroyConnection(ConnectionHandle connectionHandle, boolean causedByNetwork) {
            isConnected = false;
            ((MockSignalConnectionConnectionHandle) connectionHandle).causedByNetwork = causedByNetwork;
            ((MockSignalConnectionConnectionHandle) connectionHandle).destroy();
            disconnectLatch.countDown();
        }

        @Override
        protected ConnectionHandle executeConnectReturnConnection(SocketAddress address) throws Throwable {
            isConnected = true;
            return new MockSignalConnectionConnectionHandle();
        }

        @Override
        protected Executor getExecutorForConnection(ConnectionHandle connectionHandle) {
            return executor;
        }

        @Override
        protected ObservableFuture<Boolean> executeSend(ConnectionHandle connectionHandle, Object command) {
            sent.add((Command) command);
            return new FakeObservableFuture<Boolean>(connectionHandle, Boolean.TRUE);
        }

        private class MockSignalConnectionConnectionHandle extends SignalConnectionBaseConnectionHandleBase {

            public MockSignalConnectionConnectionHandle() {
                super(id++, MockSignalConnection.this);
            }
        }

    }

}
