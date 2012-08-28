package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.concurrent.FutureUtil;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.FakeFuture;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.InputRunnable;
import com.zipwhip.util.SocketAddressUtil;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author jed
 */
public abstract class SignalConnectionBase extends CascadingDestroyableBase implements SignalConnection {

    protected static final Logger LOGGER = Logger.getLogger(SignalConnectionBase.class);

    private final Object WRAPPER_BEING_TOUCHED_LOCK = new Object();

    public static int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 45;

    private String host = "69.46.44.181";
    //    private String host = "74.209.177.242";
    private int port = 80;
    private int connectionTimeoutSeconds = DEFAULT_CONNECTION_TIMEOUT_SECONDS;

    protected final ObservableHelper<PingEvent> pingEvent = new ObservableHelper<PingEvent>();
    protected final ObservableHelper<Command> receiveEvent = new ObservableHelper<Command>();
    protected final ObservableHelper<Boolean> connectEvent = new ObservableHelper<Boolean>();
    protected final ObservableHelper<String> exceptionEvent = new ObservableHelper<String>();
    protected final ObservableHelper<Boolean> disconnectEvent = new ObservableHelper<Boolean>();

    protected ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("SignalConnection-"));

    protected ReconnectStrategy reconnectStrategy;

    protected ChannelWrapper wrapper;
    protected final ChannelWrapperFactory channelWrapperFactory;

    private final ChannelFactory channelFactory = new OioClientSocketChannelFactory(Executors.newSingleThreadExecutor());
    private Future<Void> disconnectFuture;
    private Future<Boolean> connectFuture;

    /**
     * Protected constructor for subclasses.
     *
     * @param channelPipelineFactory The factory instantiate the ChannelPipeline
     */
    protected SignalConnectionBase(ChannelPipelineFactory channelPipelineFactory) {

        if (channelPipelineFactory == null) {
            channelPipelineFactory = new RawSocketIoChannelPipelineFactory();
        }

        if (reconnectStrategy == null) {
            this.setReconnectStrategy(reconnectStrategy);
        }

        this.channelWrapperFactory = new ChannelWrapperFactory(channelPipelineFactory, channelFactory, this);

        this.link(channelWrapperFactory);
        this.link(pingEvent);
        this.link(receiveEvent);
        this.link(connectEvent);
        this.link(exceptionEvent);
        this.link(disconnectEvent);

        // start the reconnectStrategy whenever we do a connect
        this.connectEvent.addObserver(new StartReconnectStrategyObserver(this));

        // stop the reconnectStrategy whenever we disconnect manually.
        this.disconnectEvent.addObserver(new StopReconnectStrategyObserver(this));
    }

    @Override
    public synchronized Future<Boolean> connect() throws Exception {
        LOGGER.debug(String.format("connect() : %s", Thread.currentThread().toString()));

        // are we already trying to connect?
        if (connectFuture != null) {
            return connectFuture;
        }

        if (isConnected()) {
            return new FakeFuture<Boolean>(Boolean.TRUE);
        }

        // Enforce a single connection
        validateNotConnected();

        synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
            // Enforce a single connection
            validateNotConnected();

            Asserts.assertTrue(this.wrapper == null, "We were already trying to connect when you called us. Calm down, we're working on it.");

            // prevent any side requests from any other threads from getting in.
            // TODO: If there is a crash, please don't forget to rebind the reconnectStrategy
            // TODO: What if the reconnnectStrategy has caused our connect request and we cancelled it?
            cancelAndUbindReconnectStrategy();

            // immediately/synchronously create the wrapper.
            final ChannelWrapper channelWrapper = this.wrapper = channelWrapperFactory.create();

            return connectFuture = FutureUtil.execute(this.executor, new Callable<Boolean>() {

                @Override
                public Boolean call() throws Exception {
                    boolean success;

                    try {
                        final InetSocketAddress address = SocketAddressUtil.getSingle(host, port);
                        Asserts.assertTrue(channelWrapper == wrapper, "Same instances of parent object and executor");

                        // maybe unneeded sanity check.
                        validateNotConnected();

                        try {

                            synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
                                // maybe unneeded sanity check.
                                validateNotConnected();

                                // synchronous connection to endpoint, will crash if not successful.
                                success = channelWrapper.connect(address);
                            }

                        } catch (InterruptedException e) {
                            // timeout?
                            synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
                                channelWrapper.destroy();
                                wrapper = null;
                            }

                            synchronized (SignalConnectionBase.this) {
                                connectEvent.notifyObservers(this, Boolean.FALSE);
                                disconnectEvent.notifyObservers(this, Boolean.TRUE);
                            }

                            return Boolean.FALSE;
                        }

                        // the rule is that we destroy the wrapper if it's not connected.
                        if (!success) {
                            synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
                                channelWrapper.destroy();
                                wrapper = null;
                            }
                        }

                        // restore the strategy so it can listen to these events.
                        // we are connected, so bind the reconnect strategy up.
                        bindReconnectStrategy();

                        synchronized (SignalConnectionBase.this) {
                            Asserts.assertTrue(isConnected() == channelWrapper.isConnected(), String.format("The parent and child should agree %b/%b.", isConnected(), channelWrapper.isConnected()));

                            connectEvent.notifyObservers(this, success);
                        }

                        if (!success) {
                            disconnectEvent.notifyObservers(this, Boolean.TRUE);
                        }

                        LOGGER.debug(String.format("Returning from connect. %b/%b/%b", isConnected(), channelWrapper.isConnected(), success));

                        return isConnected();
                    } finally {
                        connectFuture = null;
                    }
                }
            });
        }
    }

    @Override
    public Future<Void> disconnect() {
        return disconnect(false);
    }

    @Override
    public synchronized Future<Void> disconnect(final boolean network) {
        // if multiple requests to disconnect come in at the same time, only
        // execute it once.
        if (disconnectFuture != null) {
            // we are currently trying to disconnect?
            return disconnectFuture;
        }

        synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
            final ChannelWrapper channelWrapper = this.wrapper;

            // we can't really make any assertions on connected state.
            // isConnected is outside of our control.
            // the destroyed state IS within our control, so we can test on it.
            if (channelWrapper == null || channelWrapper.isDestroyed()) {
                // Already destroyed
                return new FakeFuture<Void>(null);
            }

            cancelAndUbindReconnectStrategy();

            LOGGER.debug("Entering the executor for .disconnect()");
            return disconnectFuture = FutureUtil.execute(executor, new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    try {
                        LOGGER.debug("Entered the executor for .disconnect()");
                        Asserts.assertTrue(wrapper == channelWrapper, "Same instance");

                        synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
                            Asserts.assertTrue(wrapper == channelWrapper, "Same instance");

                            try {
                                LOGGER.debug("Disconnecting the wrapper");
                                channelWrapper.disconnect();
                            } catch (Exception e) {
                                // odd, very odd, this shouldn't happen.
                                LOGGER.warn("Error disconnecting, the channel said", e);
                            }

                            SignalConnectionBase.this.wrapper = null;
                            try {
                                // this might crash?
                                channelWrapper.destroy();
                            } catch (Exception e) {
                                LOGGER.error("There was an error destroying the wrapper", e);
                            }
                        }

                        synchronized (SignalConnectionBase.this) {
                            if (network) {
                                bindReconnectStrategy();
                            }

                            // synchronous event firing
                            disconnectEvent.notifyObservers(this, network);
                        }

                        LOGGER.debug("Finished disconnecting the wrapper in SignalConnectionBase");

                        return null;
                    } finally {
                        disconnectFuture = null;
                    }
                }
            });
        }
    }

    @Override
    public void keepalive() {
        LOGGER.debug("Keepalive requested");
        send(PingPongCommand.getShortformInstance());
    }

    @Override
    public synchronized Future<Boolean> send(final SerializingCommand command) throws IllegalStateException {

        validateConnected();

        synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
            final ChannelWrapper w = this.wrapper;

            // send this over the wire.
            return w.write(command);
        }
    }

    /**
     * This function allows you to run tasks on the channel thread
     *
     * @param runnable
     */
    public void runIfActive(final ChannelWrapper wrapper, final Runnable runnable) {
        // this test here creates a potential deadlock because it allows
        // the Delegate to sync before the TOUCH lock. That's the wrong order.
//        synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
//            final ChannelWrapper w = this.wrapper;
//            if (w != wrapper) {
//                // they are not the same instance, they are not active. Kick them out.
//                return false;
//            } else if (w.isDestroyed()) {
//                // the wrapper is currently in the state of terminating.
//                return false;
//            }
//        }

        if (wrapper == null) {
            throw new IllegalArgumentException("The wrapper cannot be null!");
        } else if (runnable == null) {
            throw new IllegalArgumentException("The runnable cannot be null!");
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (SignalConnectionBase.this) {
                    synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
                        final ChannelWrapper w = SignalConnectionBase.this.wrapper;
                        if (w != wrapper) {
                            // they are not the same instance, they are not active.
                            // Kick them out.
                            return;
                        } else if (w.isDestroyed()) {
                            // the wrapper is currently in the state of terminating.
                            return;
                        }

                        // the wrapper is not allowed to SELF-DESTRUCT
                        // so that means that we're able to safely depend on
                        // WRAPPER BEING TOUCHED LOCK to prevent destruction between
                        // the test and the run.

                        runnable.run();
                    }
                }
            }
        });
    }

    @Override
    public boolean isConnected() {
        final ChannelWrapper w = wrapper;
        if (w == null) {
            return false;
        }

        // sanity check our assumptions.
        Asserts.assertTrue(!w.isDestroyed(), "wrapper not destroyed");

        return w.isConnected();
    }

    @Override
    public void onMessageReceived(Observer<Command> observer) {
        receiveEvent.addObserver(observer);
    }

    @Override
    public void onConnect(Observer<Boolean> observer) {
        connectEvent.addObserver(observer);
    }

    @Override
    public void onDisconnect(Observer<Boolean> observer) {
        disconnectEvent.addObserver(observer);
    }

    @Override
    public void removeOnConnectObserver(Observer<Boolean> observer) {
        connectEvent.removeObserver(observer);
    }

    @Override
    public void removeOnDisconnectObserver(Observer<Boolean> observer) {
        disconnectEvent.removeObserver(observer);
    }

    @Override
    public void onPingEvent(Observer<PingEvent> observer) {
        pingEvent.addObserver(observer);
    }

    @Override
    public void onExceptionCaught(Observer<String> observer) {
        exceptionEvent.addObserver(observer);
    }

    @Override
    public final String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public final int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectTimeoutSeconds;
    }

    @Override
    public int getConnectTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    @Override
    public ReconnectStrategy getReconnectStrategy() {
        return reconnectStrategy;
    }

    @Override
    public void setReconnectStrategy(ReconnectStrategy reconnectStrategy) {

        // Stop our old strategy
        if (this.reconnectStrategy != null) {
            this.reconnectStrategy.stop();
            this.unlink(reconnectStrategy);
        }

        this.reconnectStrategy = reconnectStrategy;
        if (this.reconnectStrategy != null) {
            this.reconnectStrategy.setSignalConnection(this);
            this.link(reconnectStrategy);
        }
    }

    protected void receivePong(PingPongCommand command) {
        if (command.isRequest()) {

            LOGGER.debug("Received a REVERSE PING");

            PingPongCommand reversePong = PingPongCommand.getNewLongformInstance();
            reversePong.setTimestamp(command.getTimestamp());
            reversePong.setToken(command.getToken());

            LOGGER.debug("Sending a REVERSE PONG");
            send(reversePong);

        } else {

            LOGGER.debug("Received a PONG");
            pingEvent.notifyObservers(this, PingEvent.PONG_RECEIVED);
        }
    }

    private void validateNotConnected() {
        if (isConnected()) {
            throw new IllegalStateException("Tried to connect but we already have a channel connected!");
        }
    }

    private synchronized void bindReconnectStrategy() {
        if (reconnectStrategy != null) {
            reconnectStrategy.start();
        }
    }

    private synchronized void cancelAndUbindReconnectStrategy() {
        if (reconnectStrategy != null) {
            reconnectStrategy.stop();
        }
    }

    private void validateConnected() {
        ChannelWrapper w = wrapper;
        if (w == null || !w.isConnected()) {
            throw new IllegalStateException("Not currently connected, expected to be!");
        }
    }

    @Override
    protected void onDestroy() {
        if (isConnected()) {
            disconnect();
        }

        if (channelFactory instanceof Destroyable) {
            ((Destroyable) channelFactory).destroy();
        } else {
            channelFactory.releaseExternalResources();
        }
    }

    /**
     * Run a callable with the current channel.
     *
     * @param callable
     */
    protected void doWithChannel(InputRunnable<ChannelWrapper> callable) {
        synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
            final ChannelWrapper w = this.wrapper;

            callable.run(w);
        }
    }

    public boolean isActive(ChannelWrapper channelWrapper) {
        final ChannelWrapper w = this.wrapper;
        return w == channelWrapper;
    }
}
