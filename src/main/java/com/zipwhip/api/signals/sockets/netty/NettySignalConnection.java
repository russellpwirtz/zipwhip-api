package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.executors.CommonExecutorFactory;
import com.zipwhip.executors.CommonExecutorTypes;
import com.zipwhip.executors.NamedThreadFactory;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Asserts;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Connects to the SignalServer via Netty over a raw socket
 */
public class NettySignalConnection extends SignalConnectionBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettySignalConnection.class);

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
    public NettySignalConnection(CommonExecutorFactory executorFactory,
                                 ReconnectStrategy reconnectStrategy,
                                 ChannelPipelineFactory channelPipelineFactory) {

        super(executorFactory != null ? executorFactory.create(CommonExecutorTypes.EVENTS, "NettySignalConnection") : null);

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

        if (executorFactory == null){

            executorFactory = new CommonExecutorFactory() {
                private Map<String, NamedThreadFactory> factories = new HashMap<String, NamedThreadFactory>();
                private NamedThreadFactory getOrCreate(String name) {
                    if (factories.containsKey(name)) {
                        return factories.get(name);
                    }
                    factories.put(name, new NamedThreadFactory(name));
                    return factories.get(name);
                }

                @Override
                public ExecutorService create(CommonExecutorTypes type, String name) {
                    switch (type) {
                        case BOSS:
                            return java.util.concurrent.Executors.newFixedThreadPool(1, getOrCreate(name + "-boss"));
                        case WORKER:
                            return java.util.concurrent.Executors.newFixedThreadPool(10, getOrCreate(name + "-worker"));
                        case EVENTS:
                            return java.util.concurrent.Executors.newFixedThreadPool(1, getOrCreate(name + "-events"));
                    }

                    throw new IllegalStateException("Not sure! " + type);
                }

                @Override
                public ExecutorService create() {
                    return create(CommonExecutorTypes.BOSS, NettySignalConnection.this.toString());
                }
            };
        }

//        channelFactory = new NioClientSocketChannelFactory(
//                executorFactory.create(CommonExecutorTypes.BOSS, "nio"),
//                executorFactory.create(CommonExecutorTypes.WORKER, "nio"), 1, 1);
        channelFactory = new OioClientSocketChannelFactory(executorFactory.create(CommonExecutorTypes.BOSS, "Netty OIO"));

        this.channelWrapperFactory = new ChannelWrapperFactory(channelPipelineFactory, channelFactory, this, executorFactory);
        this.link(channelWrapperFactory);
    }

    @Override
    protected ConnectionHandle createConnectionHandle() {
        ChannelWrapper channelWrapper = channelWrapperFactory.create();

        // this can never crash.
        ConnectionHandle connectionHandle = channelWrapper.getConnection();

        return connectionHandle;
    }

    @Override
    protected void executeConnect(ConnectionHandle connectionHandle, SocketAddress address) throws Throwable {
        ChannelWrapper channelWrapper = ((ChannelWrapperConnectionHandle)connectionHandle).channelWrapper;
        try {
            // this can crash, so be sure we clean up if it does then rethrow
            channelWrapper.connect(address);
        } catch (Throwable throwable) {
            LOGGER.error("Got exception trying to connect!", throwable);

            executeDisconnectDestroyConnection(connectionHandle, true);

            throw throwable;
        }
    }

    protected void executeDisconnectDestroyConnection(ConnectionHandle connectionHandle, boolean causedByNetwork) {
        ChannelWrapperConnectionHandle con = (ChannelWrapperConnectionHandle) connectionHandle;
        Asserts.assertTrue(!con.isDestroyed(), "Already destroyed?");
        ChannelWrapper w = con.channelWrapper;

        con.causedByNetwork = causedByNetwork;
        if (con.channelWrapper.stateManager.get() != ConnectionState.DISCONNECTED){
            con.channelWrapper.disconnect();
        }
        // dont manually trigger the disconnectFuture. The caller will do that later.


        con.destroy();

        Asserts.assertTrue(con.isDestroyed(), "Connection must be destroyed");
        Asserts.assertTrue(w.isDestroyed(), "ChannelWrapper must be destroyed");
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
        ConnectionState state = getConnectionState();
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
            try {
                executeDisconnectDestroyConnection(connectionHandle, false);
            } catch (Exception e) {
                LOGGER.error("Failed to disconnect!", e);
            }
        }

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
