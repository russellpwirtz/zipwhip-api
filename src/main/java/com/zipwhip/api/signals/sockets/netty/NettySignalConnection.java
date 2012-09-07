package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.Factory;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;

import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Connects to the SignalServer via Netty over a raw socket
 */
public class NettySignalConnection extends SignalConnectionBase {

    private static final Logger LOGGER = Logger.getLogger(NettySignalConnection.class);

    private final ChannelWrapperFactory channelWrapperFactory;
    private final ChannelFactory channelFactory;

    /**
     * Create a new {@code NettySignalConnection} with a default {@code ReconnectStrategy} and {@code ChannelPipelineFactory}.
     */
    public NettySignalConnection() {
        this(null, null);
    }

    /**
     * Create a new {@code NettySignalConnection}
     *
     * @param channelPipelineFactory The Factory to create a Netty pipeline.
     */
    public NettySignalConnection(ChannelPipelineFactory channelPipelineFactory) {
        this(null, channelPipelineFactory);
    }

    /**
     * Create a new {@code NettySignalConnection} with a default {@code ChannelPipelineFactory}.
     *
     * @param reconnectStrategy The reconnect strategy to use in the case of socket disconnects.
     */
    public NettySignalConnection(ReconnectStrategy reconnectStrategy) {
        this(reconnectStrategy, null);
    }

    /**
     * Create a new {@code NettySignalConnection}
     *
     * @param reconnectStrategy      The reconnect strategy to use in the case of socket disconnects.
     * @param channelPipelineFactory The Factory to create a Netty pipeline.
     */
    public NettySignalConnection(ReconnectStrategy reconnectStrategy, ChannelPipelineFactory channelPipelineFactory) {
        this(null, reconnectStrategy, channelPipelineFactory);
    }

    /**
     * Create a new {@code NettySignalConnection}
     *
     * @param reconnectStrategy      The reconnect strategy to use in the case of socket disconnects.
     * @param channelPipelineFactory The Factory to create a Netty pipeline.
     */
    public NettySignalConnection(Factory<ExecutorService> executorFactory, ReconnectStrategy reconnectStrategy, ChannelPipelineFactory channelPipelineFactory) {
        super(executorFactory != null ? executorFactory.create() : null);

        if (executorFactory != null){
            // We created the executor that our parent is using. We need to destroy it.
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    if (executor instanceof ExecutorService) {
                        ((ExecutorService) executor).shutdownNow();
                    }
                }
            });
        }

        if (channelPipelineFactory == null) {
            channelPipelineFactory = new RawSocketIoChannelPipelineFactory();
            this.link((Destroyable) channelPipelineFactory);
        }

        if (reconnectStrategy == null) {
            setReconnectStrategy(new DefaultReconnectStrategy());
            this.link(getReconnectStrategy());
        } else {
            setReconnectStrategy(reconnectStrategy);
        }

        Executor ex = null;
        if (executorFactory != null){
                ex = executorFactory.create();
        }

        if (ex == null) {
            ex = Executors.newSingleThreadExecutor(new NamedThreadFactory("SignalConnection-io-"));
            final Executor finalEx = ex;
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ((ExecutorService) finalEx).shutdownNow();
                }
            });
        }

        channelFactory = new OioClientSocketChannelFactory(ex);

        this.channelWrapperFactory = new ChannelWrapperFactory(channelPipelineFactory, channelFactory, this, executorFactory);
        this.link(channelWrapperFactory);
    }

    protected ConnectionHandle executeConnectReturnConnection(SocketAddress address) throws Throwable {
        ChannelWrapper channelWrapper = channelWrapperFactory.create();

        // this can never crash.
        ConnectionHandle connectionHandle = channelWrapper.getConnection();

        try {
            // this can crash, so be sure we clean up if it does then rethrow
            channelWrapper.connect(address);
        } catch (Throwable throwable) {
            Asserts.assertTrue(connectionHandle.getDisconnectFuture().isDone(), "DisconnectFuture not auto finish?");
            channelWrapper.destroy();
            throw throwable;
        }

        return connectionHandle;
    }

//    @Override
//    public synchronized ObservableFuture<Connection> connect() throws Exception {
//        LOGGER.debug(String.format("connect() : %s", Thread.currentThread().toString()));
//
//        // are we already trying to connect?
//        if (connectFuture != null) {
//            return connectFuture;
//        }
//
//        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
//            final ChannelWrapper w = wrapper;
//
//            if (w != null && !w.isDestroyed()) {
//                // in order for "wrapper" to not be null, we need to lock on it.
//                // Can someone change the wrapper without first locking on "this"??
//                return new FakeObservableFuture<Connection>(this, w.connection);
//            }
//
//            // Enforce a single connection
//            validateNotConnected();
//
//            Asserts.assertTrue(this.wrapper == null, "We were already trying to connect when you called us. Calm down, we're working on it.");
//
//            // prevent any side requests from any other threads from getting in.
//            // TODO: If there is a crash, please don't forget to rebind the reconnectStrategy
//            // TODO: What if the reconnnectStrategy has caused our connect request and we cancelled it?
//            cancelAndUbindReconnectStrategy();
//
//            // immediately/synchronously create the wrapper.
//            final ChannelWrapper channelWrapper = this.wrapper = channelWrapperFactory.create();
//            final Connection connection = channelWrapper.getConnection();
//
//            final ObservableFuture<Connection> finalConnectFuture = createSelfHealingConnectingFuture();
//
//            connectFuture = finalConnectFuture;
//
//            executor.execute(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        // maybe unneeded sanity check.
//                        Asserts.assertTrue(channelWrapper == wrapper, "Same instances of parent object and executor");
//                        Asserts.assertTrue(connection.isActive(), "Connection wasn't active!");
//                        Asserts.assertTrue(!channelWrapper.channel.isConnected(), "Channel was connected already?!");
//
//                        try {
//                            synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
//                                // maybe unneeded sanity check.
//                                Asserts.assertTrue(!channelWrapper.channel.isConnected(), "Channel was connected already?!");
//
//                                // synchronous connection to endpoint, will crash if not successful.
//                                channelWrapper.connect(getAddress());
//                            }
//
//                        } catch (Throwable throwable) {
//                            synchronized (NettySignalConnection.this) {
//                                synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
//                                    synchronized (channelWrapper.connection) {
//                                        wrapper = null;
//
//                                        channelWrapper.connection.getDisconnectFuture().setSuccess(true);
//                                        channelWrapper.destroy();
//                                    }
//                                }
//
//                                // restore the strategy so it can listen to these events.
//                                // we are connected, so bind the reconnect strategy up.
//                                bindReconnectStrategy();
//
//                                // we need to execute the disconnectEvent
//                                connectEvent.notifyObservers(connection, connection);
//                                disconnectEvent.notifyObservers(connection, connection);
//                            }
//
//                            finalConnectFuture.setFailure(throwable);
//
//                            return;
//                        }
//
//                        // restore the strategy so it can listen to these events.
//                        // we are connected, so bind the reconnect strategy up.
//                        bindReconnectStrategy();
//
//                        synchronized (NettySignalConnection.this) {
//                            Asserts.assertTrue(isConnected() == !channelWrapper.isDestroyed(), String.format("The parent and child should agree %b/%b.", isConnected(), !channelWrapper.isDestroyed()));
//
//                            connectEvent.notifyObservers(connection, connection);
//                        }
//
//                        LOGGER.debug(String.format("Returning from connect. %b/%b", isConnected(), !channelWrapper.isDestroyed()));
//
//                        finalConnectFuture.setSuccess(connection);
//                    } catch (RuntimeException e) {
//                        finalConnectFuture.setFailure(e);
//                        throw e;
//                    } finally {
//                        // make sure we cleaned it up!
//                        Asserts.assertTrue(finalConnectFuture.isDone(), "ConnectFuture left dangling?");
//                    }
//                }
//            });
//
//            return finalConnectFuture;
//        }
//    }

//    @Override
//    public synchronized ObservableFuture<Connection> disconnect(final boolean network) {
//        // if multiple requests to disconnect come in at the same time, only
//        // execute it once.
//        if (disconnectFuture != null) {
//            // we are currently trying to disconnect?
//            return disconnectFuture;
//        }
//
//        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
//            final ChannelWrapper channelWrapper = this.wrapper;
//            final Connection connection = channelWrapper.connection;
//
//            // we can't really make any assertions on connected state.
//            // isConnected is outside of our control.
//            // the destroyed state IS within our control, so we can test on it.
//            if (channelWrapper == null || channelWrapper.isDestroyed()) {
//                // Already in a "disconnected" state
//                return new FakeFailingObservableFuture<Connection>(this, new IllegalStateException("Already disconnected."));
//            }
//
//            cancelAndUbindReconnectStrategy();
//
//            final ObservableFuture<Connection> finalDisconnectFuture = createSelfHealingDisconnectingFuture();
//
//            disconnectFuture = finalDisconnectFuture;
//
//            executor.execute(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        LOGGER.debug("Entered the executor for .disconnect()");
//                        Asserts.assertTrue(wrapper == channelWrapper, "Same instance");
//
//                        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
//                            Asserts.assertTrue(wrapper == channelWrapper, "Same instance");
//
//                            try {
//                                LOGGER.debug("Disconnecting the wrapper");
//                                channelWrapper.disconnect();
//                            } catch (Exception e) {
//                                // odd, very odd, this shouldn't happen.
//                                LOGGER.warn("Error disconnecting, the channel said", e);
//                            }
//
//                            NettySignalConnection.this.wrapper = null;
//                            try {
//                                // this might crash?
//                                channelWrapper.destroy();
//                            } catch (Exception e) {
//                                LOGGER.error("There was an error destroying the wrapper", e);
//                            }
//                        }
//
//                        synchronized (NettySignalConnection.this) {
//                            if (network) {
//                                bindReconnectStrategy();
//                            }
//
//                            channelWrapper.connection.getDisconnectFuture().setSuccess(network);
//
//                            // synchronous event firing
//                            disconnectEvent.notifyObservers(connection, connection);
//                        }
//
//                        LOGGER.debug("Finished disconnecting the wrapper in SignalConnectionBase");
//
//                        finalDisconnectFuture.setSuccess(connection);
//                    } catch (RuntimeException e) {
//                        finalDisconnectFuture.setFailure(e);
//                        throw e;
//                    }
//                }
//            });
//
//            return finalDisconnectFuture;
//        }
//    }

    protected void executeDisconnectDestroyConnection(ConnectionHandle connectionHandle, boolean causedByNetwork) {
        ChannelWrapperConnectionHandle con = (ChannelWrapperConnectionHandle) connectionHandle;

        con.causedByNetwork = causedByNetwork;
        con.channelWrapper.disconnect();
        // dont manually trigger the disconnectFuture. The caller will do that later.

        con.destroy();
    }

    protected ObservableFuture<Boolean> executeSend(ConnectionHandle connectionHandle, final Object command) {
        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
            validateConnected();

            return ((ChannelWrapperConnectionHandle) connectionHandle).write(command);
        }
    }

    @Override
    protected Executor getExecutorForConnection(ConnectionHandle connectionHandle) {
        return ((ChannelWrapperConnectionHandle)connectionHandle).channelWrapper.executor;
    }

    @Override
    protected void onDestroy() {
        if (channelFactory instanceof Destroyable) {
            ((Destroyable) channelFactory).destroy();
        } else {
            channelFactory.releaseExternalResources();
        }
    }

    @Override
    protected <T> void notifyEvent(final ConnectionHandle connectionHandle, final ObservableHelper<T> event, final T data) {
        ((ChannelWrapperConnectionHandle) connectionHandle).channelWrapper.executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (NettySignalConnection.this) {
                    synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
                        synchronized (connectionHandle) {
                            event.notifyObservers(connectionHandle, data);
                        }
                    }
                }
            }
        });
    }
}
