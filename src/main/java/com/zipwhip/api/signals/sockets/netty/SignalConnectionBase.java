package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.ApiConnectionConfiguration;
import com.zipwhip.api.NestedObservableFuture;
import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.FakeFailingObservableFuture;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Asserts;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author jed
 */
public abstract class SignalConnectionBase extends CascadingDestroyableBase implements SignalConnection {

    private static final Logger LOGGER = Logger.getLogger(SignalConnectionBase.class);

    public static int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 45;

    protected final ObservableHelper<PingEvent> pingEvent = new ObservableHelper<PingEvent>();
    protected final ObservableHelper<Command> receiveEvent = new ObservableHelper<Command>();
    protected final ObservableHelper<String> exceptionEvent = new ObservableHelper<String>();
    protected final ObservableHelper<ConnectionHandle> connectEvent = new ObservableHelper<ConnectionHandle>();
    protected final ObservableHelper<ConnectionHandle> disconnectEvent = new ObservableHelper<ConnectionHandle>();

    protected final Executor executor;
    protected final Object CONNECTION_BEING_TOUCHED_LOCK = new Object();

    private ReconnectStrategy reconnectStrategy;
    private int connectionTimeoutSeconds = DEFAULT_CONNECTION_TIMEOUT_SECONDS;
    private SocketAddress address = new InetSocketAddress(ApiConnectionConfiguration.SIGNALS_HOST, ApiConnectionConfiguration.SIGNALS_PORT);

    // this is how we prevent redundant requests to disconnect/connect.
    // NOTE: Maybe it's a good thing that you can disconnect twice??
    private ObservableFuture<ConnectionHandle> disconnectFuture;
    protected ObservableFuture<ConnectionHandle> connectFuture;
    protected ConnectionHandle connectionHandle;

    /**
     * Protected constructor for subclasses.
     */
    protected SignalConnectionBase(Executor executor) {

        if (executor != null) {
            this.executor = executor;
        } else {
            this.executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("SignalConnection-"));
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ((ExecutorService) SignalConnectionBase.this.executor).shutdownNow();
                }
            });
        }

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
    public synchronized ObservableFuture<ConnectionHandle> connect() {
        if (isDestroyed()) {
            throw new IllegalStateException("The connection is destroyed");
        } else if (connectFuture != null) {
            return connectFuture;
        } else if (disconnectFuture != null) {
            throw new IllegalStateException("Currently already trying to disconnect");
        }

        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
            final ObservableFuture<ConnectionHandle> finalConnectingFuture = createSelfHealingConnectingFuture();
            connectFuture = finalConnectingFuture;

            cancelAndUbindReconnectStrategy();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (finalConnectingFuture) {
                        if (finalConnectingFuture.isCancelled()) {
                            LOGGER.warn("Our connectingFutur was cancelled. Quitting.");
                            return;
                        }
                    }

                    ConnectionHandle connectionHandle;

                    synchronized (CONNECTION_BEING_TOUCHED_LOCK) {

                        try {
                            connectionHandle = executeConnectReturnConnection(getAddress());

                            synchronized (finalConnectingFuture) {
                                if (finalConnectingFuture.isCancelled()) {
                                    LOGGER.error("While connecting they cancelled!");
                                    executeDisconnectDestroyConnection(connectionHandle, false);
                                    return;
                                }
                            }

                            // setting this as this.connection makes it "current"
                            SignalConnectionBase.this.connectionHandle = connectionHandle;
                        } catch (Throwable e) {
                            synchronized (finalConnectingFuture) {
                                if (finalConnectingFuture.isCancelled()) {
                                    // TODO: what does this do for the ReconnectStrategy? Has it been unbound?
                                    LOGGER.error("While connecting they cancelled!");
                                    return;
                                }
                            }

                            // setting this as this.connection makes it "current"
                            SignalConnectionBase.this.connectionHandle = null;

                            bindReconnectStrategy();

                            finalConnectingFuture.setFailure(e);
                            return;
                        }
                    }

                    bindReconnectStrategy();

                    if (connectFuture == finalConnectingFuture) {
                        connectFuture = null;
                    }

                    // throw the connectEvent BEFORE we finish the future.
                    connectEvent.notifyObservers(connectionHandle, connectionHandle);
                    finalConnectingFuture.setSuccess(connectionHandle);
                }
            });

            return finalConnectingFuture;
        }
    }

    @Override
    public ObservableFuture<ConnectionHandle> disconnect() {
        return disconnect(false);
    }

    public synchronized ObservableFuture<ConnectionHandle> disconnect(ConnectionHandle connectionHandle, boolean causedByNetwork) {
        if (connectionHandle == null) {
            throw new NullPointerException("Connection cannot be null");
        }

        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
            synchronized (connectionHandle) {
                if (connectionHandle.isDestroyed()) {
                    throw new IllegalStateException("The connection cannot be destroyed");
                } else if (connectionHandle.getDisconnectFuture().isDone()) {
                    return connectionHandle.getDisconnectFuture();
                }

                if (getConnectionHandle() == connectionHandle) {
                    return disconnect(causedByNetwork);
                } else {
                    throw new IllegalStateException("How can the future not be done, but not currently active?");
                }
            }
        }
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect(final boolean causedByNetwork) {
        if (isDestroyed()) {
            throw new IllegalStateException("The connection is destroyed");
        }

        switch (getConnectionState()) {
            case CONNECTING:
                final ObservableFuture f = connectFuture;
                connectFuture = null;
                f.cancel();
                break;
            case CONNECTED:
                break;
            case DISCONNECTED:
            case DISCONNECTING:
                return disconnectFuture;
            default:
                throw new IllegalStateException("Not possible!");
        }


        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
            final ConnectionHandle connectionHandle = this.connectionHandle;

            Asserts.assertTrue(connectionHandle != null, "The connectionHandle was null even though we did a safe state check!");

            synchronized (connectionHandle) {
                final ObservableFuture<ConnectionHandle> finalDisconnectingFuture = connectionHandle.getDisconnectFuture();

                Asserts.assertTrue(!finalDisconnectingFuture.isDone(), "The disconnectingFuture can't be done!");

                cancelAndUbindReconnectStrategy();

                disconnectFuture = finalDisconnectingFuture;

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (finalDisconnectingFuture) {
                            if (finalDisconnectingFuture.isCancelled()) {
                                LOGGER.error("The disconnectFuture was cancelled, so we're quitting.");
                                return;
                            }

                            synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
                                synchronized (connectionHandle) {
                                    // we can't call connection.execute() because it would be
                                    // an infinite loop. We need to let our base class (who created this guy)
                                    // to handle the synchronous disconnection.
                                    executeDisconnectDestroyConnection(connectionHandle, causedByNetwork);
                                    SignalConnectionBase.this.connectionHandle = null;
                                }
                            }

                            synchronized (SignalConnectionBase.this) {
                                synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
                                    if (disconnectFuture == finalDisconnectingFuture) {
                                        disconnectFuture = null;
                                    }

                                    bindReconnectStrategy();

                                    finalDisconnectingFuture.setSuccess(connectionHandle);
                                    disconnectEvent.notifyObservers(connectionHandle, connectionHandle);
                                }
                            }
                        }
                    }
                });

                return finalDisconnectingFuture;
            }
        }
    }

    @Override
    public void ping() {
        LOGGER.debug("Keepalive requested");
        send(PingPongCommand.getShortformInstance());
    }

    @Override
    public ObservableFuture<ConnectionHandle> reconnect() {
        final NestedObservableFuture<ConnectionHandle> resultFuture = new NestedObservableFuture<ConnectionHandle>(this);

        disconnect().addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> disconnectFuture) {

                try {
                    ObservableFuture<ConnectionHandle> connectFuture = connect();

                    resultFuture.setNestedFuture(connectFuture);
                } catch (Exception e) {
                    resultFuture.setFailure(e);
                }
            }
        });

        return resultFuture;
    }

    @Override
    public synchronized ObservableFuture<Boolean> send(final SerializingCommand command) {
        return executeSend(getConnectionHandle(), command);
    }

    protected abstract void executeDisconnectDestroyConnection(ConnectionHandle connectionHandle, boolean causedByNetwork);


    protected abstract ConnectionHandle executeConnectReturnConnection(SocketAddress address) throws Throwable;


    /**
     * For throwing events
     *
     * @param connectionHandle
     * @param observableHelper
     * @param data
     * @param <T>
     */
    protected <T> void runIfActive(final ConnectionHandle connectionHandle, final ObservableHelper<T> observableHelper, final T data) {
        runIfActive(connectionHandle, getExecutorForConnection(connectionHandle), new Runnable() {
            @Override
            public void run() {
                observableHelper.notifyObservers(connectionHandle, data);
            }
        });
    }

    /**
     * This function allows you to run tasks on the channel thread
     *
     * @param runnable
     */
    protected void runIfActive(final ConnectionHandle connectionHandle, final Runnable runnable) {
        runIfActive(connectionHandle, executor, runnable);
    }

    // TODO: actually dont crash here. It takes down the phone and isn't necessary.
    protected void runIfActive(final ConnectionHandle connectionHandle, Executor executor, final Runnable runnable) {
        if (connectionHandle == null) {
            LOGGER.error("The connectionHandle was null, so most certainly was not active. Quitting");
            return;
        } else if (runnable == null) {
            throw new NullPointerException("The runnable can never be null.");
        } else if (connectionHandle != this.getConnectionHandle()) {
            LOGGER.error(String.format("The connectionHandle %s was not the same as %s. Quitting.", connectionHandle, getConnectionHandle()));
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (SignalConnectionBase.this) {
                    synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
                        final ConnectionHandle w = getConnectionHandle();
                        if (w != connectionHandle) {
                            // they are not the same instance, they are not active.
                            // Kick them out.
                            LOGGER.error(String.format("The connectionHandle %s was not the same as %s. Quitting.", connectionHandle, getConnectionHandle()));
                        } else if (w.isDestroyed()) {
                            // the wrapper is currently in the state of terminating.
                            LOGGER.error(String.format("The connectionHandle %s was destroyed. Quitting.", w));
                            return;
                        }

                        // he wrapper is not allowed to SELF-DESTRUCT
                        // so that means that we're able to safely depend on
                        // WRAPPER BEING TOUCHED LOCK to prevent destruction between
                        // the test and the run.
                        runnable.run();
                    }
                }
            }
        });
    }


    protected abstract Executor getExecutorForConnection(ConnectionHandle connectionHandle);

    protected void send(final ConnectionHandle connectionHandle, final Object command) {
        runIfActive(connectionHandle, getExecutorForConnection(connectionHandle), new Runnable() {

            @Override
            public void run() {
                executeSend(connectionHandle, command);
            }
        });
    }

    protected abstract ObservableFuture<Boolean> executeSend(ConnectionHandle connectionHandle, final Object command);

    protected void receivePong(final ConnectionHandle connectionHandle, final PingPongCommand command) {

        getExecutorForConnection(connectionHandle).execute(new Runnable() {
            @Override
            public void run() {
                synchronized (connectionHandle) {
                    if (connectionHandle.isDestroyed()) {
                        return;
                    }

                    Asserts.assertTrue(connectionHandle == getConnectionHandle(), "Current connection not matching?!?");

                    if (command.isRequest()) {
                        LOGGER.debug("Received a REVERSE PING");

                        PingPongCommand reversePong = PingPongCommand.getNewLongformInstance();
                        reversePong.setTimestamp(command.getTimestamp());
                        reversePong.setToken(command.getToken());

                        LOGGER.debug("Sending a REVERSE PONG");
                        send(connectionHandle, reversePong);
                    } else {
                        LOGGER.debug("Received a PONG");

                        pingEvent.notifyObservers(connectionHandle, PingEvent.PONG_RECEIVED);
                    }
                }
            }
        });
    }

    protected synchronized void bindReconnectStrategy() {
        if (reconnectStrategy != null) {
            reconnectStrategy.start();
        }
    }

    protected synchronized void cancelAndUbindReconnectStrategy() {
        if (reconnectStrategy != null) {
            reconnectStrategy.stop();
        }
    }

    protected void validateConnected() {
        if (getConnectionState() != ConnectionState.CONNECTED) {
            throw new IllegalStateException("Not currently connected, expected to be!");
        }
    }

    @Override
    protected void onDestroy() {
        final ConnectionState connectionState = getConnectionState();
        if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
            try {
                disconnect();
            } catch (Exception e) {
                // eeks!
            }
        }
    }

    public synchronized ConnectionState getConnectionState() {
        // The futures cannot change without a synchronization on "SignalConnection"

        if (connectFuture != null && !connectFuture.isDone() && !connectFuture.isCancelled()) {
            return ConnectionState.CONNECTING;
        } else if (disconnectFuture != null && !disconnectFuture.isDone() && !disconnectFuture.isCancelled()) {
            return ConnectionState.DISCONNECTING;
        }

        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
            // you cannot change the current ConnectionHandle while holding the CONNECTION_BEING_TOUCHED_LOCK
            final ConnectionHandle finalConnectionHandle = getConnectionHandle();
            if (finalConnectionHandle == null || finalConnectionHandle.isDestroyed()) {
                return ConnectionState.DISCONNECTED;
            } else {
                return ConnectionState.CONNECTED;
            }
        }
    }

    public ConnectionHandle getConnectionHandle() {
        return connectionHandle;
    }

    protected ObservableFuture<ConnectionHandle> createSelfHealingConnectingFuture() {
        ObservableFuture<ConnectionHandle> future = new DefaultObservableFuture<ConnectionHandle>(this) {
            @Override
            public String toString() {
                return "finalConnectFuture";
            }
        };

        // if this future is auto finished (called too early) we fail to reset the connectFuture.
        // make sure this doesn't get called synchronously right now!
        final Object a = connectFuture;
        future.addObserver(resetConnectFutureIfActiveObserver);
        Asserts.assertTrue(a == connectFuture || connectFuture != null, "Did your future finish too early?!?");

        return future;
    }

    protected NestedObservableFuture<ConnectionHandle> createSelfHealingDisconnectingFuture() {
        NestedObservableFuture<ConnectionHandle> future = new NestedObservableFuture<ConnectionHandle>(this) {
            @Override
            public String toString() {
                return "SignalConnection/disconnectFuture";
            }
        };

        final Object o = disconnectFuture;
        future.addObserver(resetDisconnectFutureIfActiveObserver);
        Asserts.assertTrue(o == disconnectFuture || disconnectFuture != null, "Did your observer fire too early??");

        return future;
    }

    private final Observer<ObservableFuture<ConnectionHandle>> resetConnectFutureIfActiveObserver = new Observer<ObservableFuture<ConnectionHandle>>() {
        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> finalConnectingFuture) {
            if (connectFuture == finalConnectingFuture) {
                connectFuture = null;
            }
        }
    };

    private final Observer<ObservableFuture<ConnectionHandle>> resetDisconnectFutureIfActiveObserver = new Observer<ObservableFuture<ConnectionHandle>>() {
        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> future) {
            if (disconnectFuture == future) {
                disconnectFuture = null;
            }
        }
    };

    protected <T> void notifyEvent(ConnectionHandle connectionHandle, ObservableHelper<T> event, T data) {
        synchronized (this) {
            synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
                synchronized (connectionHandle) {
                    event.notifyObservers(connectionHandle, data);
                }
            }
        }
    }

    protected ObservableFuture<ConnectionHandle> reconnect(ConnectionHandle connectionHandle) {
        synchronized (this) {
            if (getConnectionHandle() == connectionHandle) {
                return reconnect();
            } else {
                return new FakeFailingObservableFuture<ConnectionHandle>(connectionHandle, new Throwable("Not the current connection"));
            }
        }
    }


    @Override
    public Observable<Command> getCommandReceivedEvent() {
        return receiveEvent;
    }

    public Observable<ConnectionHandle> getConnectEvent() {
        return connectEvent;
    }

    public Observable<ConnectionHandle> getDisconnectEvent() {
        return disconnectEvent;
    }

    @Override
    public Observable<PingEvent> getPingEventReceivedEvent() {
        return pingEvent;
    }

    public Observable<String> getExceptionEvent() {
        return exceptionEvent;
    }

    public void setAddress(SocketAddress address) {
        this.address = address;
    }

    public SocketAddress getAddress() {
        return this.address;
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
}
