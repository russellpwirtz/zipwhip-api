package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.concurrent.ConfiguredFactory;
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
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;

import java.net.SocketAddress;
import java.util.concurrent.*;

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
    public NettySignalConnection(ConfiguredFactory<String, ExecutorService> executorFactory, ReconnectStrategy reconnectStrategy, ChannelPipelineFactory channelPipelineFactory) {
        super(executorFactory != null ? executorFactory.create("SignalConnection") : null);

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
            executorFactory = new ConfiguredFactory<String, ExecutorService>() {
                @Override
                public ExecutorService create(String threadName) {
                    return Executors.newFixedThreadPool(1, new NamedThreadFactory("SignalConnection-"));
                }
            };
        }

        channelFactory = new OioClientSocketChannelFactory(executorFactory.create("OioClientSocketChannelWorker"));

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
