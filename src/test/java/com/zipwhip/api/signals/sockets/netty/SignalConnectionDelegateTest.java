package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.FakeFuture;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static junit.framework.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 8/27/12
 * Time: 2:42 PM
 */
public class SignalConnectionDelegateTest {

    SignalConnectionDelegate delegate;
    SignalConnectionBase connection;

    // The delegate does disconnect async so we need this to control timing
    CountDownLatch disconnectLatch;

    @Before
    public void setUp() throws Exception {
        connection = new MockSignalConnection();
        delegate = new SignalConnectionDelegate(connection);
        disconnectLatch = new CountDownLatch(1);
    }

    @Test
    public void testDisconnectIsDestroyed() throws Exception {

        assertFalse(delegate.isDestroyed());

        delegate.destroy();
        assertTrue(delegate.isDestroyed());

        connection.connect().get();
        assertTrue(connection.isConnected());

        // Disconnect fails because we are destroyed
        delegate.disconnect(true);

        assertTrue(delegate.isConnected());
    }

    @Test
    public void testDisconnectNetwork() throws Exception {

        assertFalse(delegate.isDestroyed());

        connection.connect().get();
        assertTrue(connection.isConnected());

        // Network
        delegate.disconnect(true);
        disconnectLatch.await();

        assertFalse(connection.isConnected());
        assertFalse(delegate.isConnected());
    }

    @Test
    public void testDisconnectNonNetwork() throws Exception {

        assertFalse(delegate.isDestroyed());

        connection.connect().get();
        assertTrue(connection.isConnected());

        // Non-network
        delegate.disconnect(false);
        disconnectLatch.await();

        assertFalse(connection.isConnected());
        assertFalse(delegate.isConnected());
    }

    @Test
    public void testIsConnected() throws Exception {

        connection.connect().get();
        assertTrue(connection.isConnected());
        assertTrue(delegate.isConnected());

        delegate.disconnect(false);
        disconnectLatch.await();

        assertFalse(connection.isConnected());
        assertFalse(delegate.isConnected());
    }

    @Test
    public void testSend() throws Exception {

        assertFalse(delegate.isDestroyed());
        delegate.send(PingPongCommand.getShortformInstance());
        delegate.send(PingPongCommand.getShortformInstance());
        delegate.send(PingPongCommand.getShortformInstance());

        assertEquals(3, ((MockSignalConnection)connection).getSent().size());
    }

    @Test
    public void testReceivePong() throws Exception {

        assertFalse(delegate.isDestroyed());
        delegate.receivePong(PingPongCommand.getShortformInstance());
        delegate.receivePong(PingPongCommand.getShortformInstance());
        delegate.receivePong(PingPongCommand.getShortformInstance());

        assertEquals(3, ((MockSignalConnection)connection).getPongs().size());
    }

    private class MockSignalConnection extends SignalConnectionBase implements SignalConnection {

        protected ExecutorService executor;

        // We need these to block for testing
        protected final List<Observer<Command>> receiveEvent = new ArrayList<Observer<Command>>();
        protected final List<Observer<Boolean>> connectEvent = new ArrayList<Observer<Boolean>>();
        protected final List<Observer<Boolean>> disconnectEvent = new ArrayList<Observer<Boolean>>();

        protected final List<Command> sent = new ArrayList<Command>();
        protected final List<PingPongCommand> pongs = new ArrayList<PingPongCommand>();

        protected boolean isConnected = false;

        public MockSignalConnection() {
            super(null);
        }

        @Override
        public synchronized Future<Boolean> connect() throws Exception {

            FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

                @Override
                public Boolean call() throws Exception {
                    isConnected = true;

                    for (Observer<Boolean> o : connectEvent) {
                        o.notify(this, isConnected);
                    }

                    for (Observer<Command> o : receiveEvent) {
                        o.notify(this, new ConnectCommand("1234-5678-1234-5678", null));
                    }

                    return isConnected;
                }
            });

            if (executor == null) {
                executor = Executors.newSingleThreadExecutor();
            }
            executor.execute(task);

            return task;
        }

        @Override
        public synchronized Future<Void> disconnect() {
            return disconnect(false);
        }

        @Override
        public Future<Void> disconnect(final boolean requestReconnect) {

            FutureTask<Void> task = new FutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() throws Exception {

                    isConnected = false;
                    executor.shutdownNow();
                    executor = null;

                    for (Observer<Boolean> o : disconnectEvent) {
                        o.notify(this, requestReconnect);
                    }

                    disconnectLatch.countDown();

                    return null;
                }
            });

            executor.execute(task);
            return task;
        }

        @Override
        public Future<Boolean> send(SerializingCommand command) {
            sent.add(command);
            return new FakeFuture<Boolean>(true);
        }

        @Override
        public void keepalive() {

        }

        @Override
        public boolean isConnected() {
            return isConnected;
        }

        @Override
        public void onMessageReceived(Observer<Command> observer) {
            receiveEvent.add(observer);
        }

        @Override
        public void onConnect(Observer<Boolean> observer) {
            connectEvent.add(observer);
        }

        @Override
        public void onDisconnect(Observer<Boolean> observer) {
            disconnectEvent.add(observer);
        }

        @Override
        public void removeOnConnectObserver(Observer<Boolean> observer) {
            connectEvent.remove(observer);
        }

        @Override
        public void removeOnDisconnectObserver(Observer<Boolean> observer) {
            disconnectEvent.remove(observer);
        }

        @Override
        public void onPingEvent(Observer<PingEvent> observer) {
        }

        @Override
        public void onExceptionCaught(Observer<String> observer) {
        }

        @Override
        public void setHost(String host) {
        }

        @Override
        public void setPort(int port) {
        }

        @Override
        public ReconnectStrategy getReconnectStrategy() {
            return null;
        }

        @Override
        public void setReconnectStrategy(ReconnectStrategy strategy) {
        }

        @Override
        public void destroy() {
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }

        @Override
        protected void receivePong(PingPongCommand command) {
            pongs.add(command);
        }

        public final List<Command> getSent() {
            return sent;
        }

        public final List<PingPongCommand> getPongs() {
            return pongs;
        }

    }

}
