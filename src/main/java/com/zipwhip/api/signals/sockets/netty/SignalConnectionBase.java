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
import com.zipwhip.concurrent.*;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.FakeObservableFuture;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Asserts;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.zipwhip.concurrent.ThreadUtil.ensureLock;

/**
 * @author jed
 */
public abstract class SignalConnectionBase extends CascadingDestroyableBase implements SignalConnection {

    private static final Logger LOGGER = Logger.getLogger(SignalConnectionBase.class);

    public static int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 10;

    protected final ObservableHelper<PingEvent> pingEvent = new DebugObservableHelper<PingEvent>(new ObservableHelper<PingEvent>()) {
        @Override
        public String toString() {
            return "pingEvent";
        }
    };
    protected final ObservableHelper<Command> receiveEvent = new DebugObservableHelper<Command>(new ObservableHelper<Command>()) {
        @Override
        public String toString() {
            return "receiveEvent";
        }
    };
    protected final ObservableHelper<String> exceptionEvent = new DebugObservableHelper<String>(new ObservableHelper<String>()) {
        @Override
        public String toString() {
            return "exceptionEvent";
        }
    };
    protected final ObservableHelper<ConnectionHandle> connectEvent = new DebugObservableHelper<ConnectionHandle>(new ObservableHelper<ConnectionHandle>()) {
        @Override
        public String toString() {
            return "connectEvent";
        }
    };
    protected final ObservableHelper<ConnectionHandle> disconnectEvent = new DebugObservableHelper<ConnectionHandle>(new ObservableHelper<ConnectionHandle>()) {
        @Override
        public String toString() {
            return "disconnectEvent";
        }
    };

    protected final Executor executor;
    protected final Object CONNECTION_BEING_TOUCHED_LOCK = new Object() {
        @Override
        public String toString() {
            return "CONNECTION_BEING_TOUCHED_LOCK";
        }
    };

    private ReconnectStrategy reconnectStrategy;
    private int connectionTimeoutSeconds = DEFAULT_CONNECTION_TIMEOUT_SECONDS;
    private SocketAddress address = new InetSocketAddress(ApiConnectionConfiguration.SIGNALS_HOST, ApiConnectionConfiguration.SIGNALS_PORT);

    // this is how we prevent redundant requests to disconnect/connect.
    // NOTE: Maybe it's a good thing that you can disconnect twice??
    protected ObservableFuture<ConnectionHandle> disconnectFuture;
    protected ObservableFuture<ConnectionHandle> connectFuture;
    protected ConnectionHandle connectionHandle;

    /**
     * Protected constructor for subclasses.
     */
    protected SignalConnectionBase(Executor executor) {

        if (executor != null) {
            this.executor = executor;
        } else {
            this.executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("SignalConnection(newSingleThreadExecutor)-"));
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
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> connect() {
        LOGGER.debug("connect()");
        if (isDestroyed()) {
            throw new IllegalStateException("The connection is destroyed");
        } else if (connectFuture != null) {
            return connectFuture;
        }

        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
            ConnectionState state = getConnectionState();

            switch (state) {
                case CONNECTING:
                    return connectFuture;
                case CONNECTED:
                    return new FakeObservableFuture<ConnectionHandle>(this, connectionHandle);
            }

            if (disconnectFuture != null) {
                synchronized (disconnectFuture) {
                    cancelDisconnectFuture();
                }
            }

            boolean shouldRebindReconnectStrategy;
            final ObservableFuture<ConnectionHandle> finalConnectFuture = createSelfHealingConnectingFuture();
            synchronized (finalConnectFuture) {
                setConnectFuture(finalConnectFuture);

                shouldRebindReconnectStrategy = cancelAndUbindReconnectStrategy();

//                finalConnectFuture.addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
//                    @Override
//                    public void notify(Object sender, ObservableFuture<ConnectionHandle> future) {
//                        synchronized (SignalConnectionBase.this) {
//                            synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
//                                synchronized (future) {
//                                    // when the connection finishes for any reason (cancelled or error)
//                                    // rebind the reconnect strategy since we disconnected it.
//                                    bindReconnectStrategy();
//                                }
//                            }
//                        }
//                    }
//                });
            }

            final boolean finalShouldRebindReconnectStrategy = shouldRebindReconnectStrategy;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (finalConnectFuture) {
                        if (finalConnectFuture.isCancelled()) {
                            LOGGER.warn("Our connectingFutur was cancelled. Quitting.");
                            return;
                        }
                    }

                    ConnectionHandle connectionHandle = createConnectionHandle();

                    try {
                        executeConnect(connectionHandle, getAddress());

                        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
                            synchronized (finalConnectFuture) {
                                if (finalConnectFuture.isCancelled()) {
                                    LOGGER.error("While connecting they cancelled! 1");
                                    executeDisconnectDestroyConnection(connectionHandle, false);
                                    return;
                                }

                                synchronized (connectionHandle) {
                                    if (connectionHandle.isDestroyed() || connectionHandle.getDisconnectFuture().isDone()) {
                                        throw new IllegalStateException("This connection was in an invalid state.");
                                    }
                                    // we have to hold the lock in order to change the active one.

                                    // setting this as this.connection makes it "current"
                                    setConnectionHandle(connectionHandle);

                                    clearConnectFutureIfSame(finalConnectFuture);
                                    bindReconnectStrategy();
                                }
                            }
                        }
                    } catch (Throwable e) {
                        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
                            synchronized (finalConnectFuture) {
                                if (finalConnectFuture.isCancelled()) {
                                    // TODO: what does this do for the ReconnectStrategy? Has it been unbound?
                                    LOGGER.error("While connecting they cancelled! 2");
                                    return;
                                }

                                // setting this as this.connection makes it "current"
                                clearConnectionHandle(connectionHandle);

                                clearConnectFutureIfSame(finalConnectFuture);
                                if (finalShouldRebindReconnectStrategy) {
                                    bindReconnectStrategy();
                                }

                                disconnectEvent.notifyObservers(connectionHandle, connectionHandle);
                                finalConnectFuture.setFailure(e);
                            }
                        }
                        return;
                    }

                    synchronized (finalConnectFuture) {
                        if (finalConnectFuture.isCancelled()) {
                            return;
                        }

                        connectEvent.notifyObservers(connectionHandle, connectionHandle);
                        finalConnectFuture.setSuccess(connectionHandle);
                    }
                }
            });

            return finalConnectFuture;
        }
    }


    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect(final boolean causedByNetwork) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("disconnect(%b)", causedByNetwork));
        }

        if (isDestroyed()) {
            throw new IllegalStateException("The connection is destroyed");
        }

        synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
            ConnectionState state = getConnectionState();
            switch (state) {
                case CONNECTING:
                    accessConnectFuture();
                    synchronized (connectFuture) {
                        cancelConnectFuture();
                        if (connectionHandle == null) {
                            if (causedByNetwork) {
                                // Since we cancelled the connectFuture, their rebindReonnectStrategy() has also been
                                // cancelled. We need to bind it up again.
                                bindReconnectStrategy();
                            }

                            // it's possible that we're already disconnected (haven't gotten to the
                            // connect part yet). We have to synchronize around the future because then our
                            // cancellation will take effect.
                            // TODO: is this a bug?
                            // TODO: fire the disconnectEvent?
                            // TODO: return the disconnectFuture?
                            return new FakeObservableFuture<ConnectionHandle>(null, null);
                        }
                    }
                    break;
                case CONNECTED:
                    break;

                case DISCONNECTED:
                    return new FakeObservableFuture<ConnectionHandle>(null, null);
                case DISCONNECTING:
                    accessDisconnectFuture();
                    return disconnectFuture;
                default:
                    throw new IllegalStateException("Not possible! " + state);
            }

            final ConnectionHandle connectionHandle = this.connectionHandle;

            Asserts.assertTrue(connectionHandle != null, "The connectionHandle was null even though we did a safe state check! " + state);

            synchronized (connectionHandle) {
                final ObservableFuture<ConnectionHandle> finalDisconnectingFuture = connectionHandle.getDisconnectFuture();
                synchronized (finalDisconnectingFuture) {
                    if (finalDisconnectingFuture.isCancelled()) {
                        throw new IllegalStateException("How can this already be cancelled? " + finalDisconnectingFuture);
                    }

                    Asserts.assertTrue(!finalDisconnectingFuture.isDone(), "The disconnectingFuture can't be done!");

                    cancelAndUbindReconnectStrategy();

                    setDisconnectFuture(finalDisconnectingFuture);
                }

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (SignalConnectionBase.this) {
                            synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
                                synchronized (finalDisconnectingFuture) {
                                    if (finalDisconnectingFuture.isCancelled()) {
                                        LOGGER.error("The disconnectFuture was cancelled, so we're quitting.");
                                        return;
                                    }

                                    synchronized (connectionHandle) {

                                        // we can't call connection.execute() because it would be
                                        // an infinite loop. We need to let our base class (who created this guy)
                                        // to handle the synchronous disconnection.
                                        try {
                                            executeDisconnectDestroyConnection(connectionHandle, causedByNetwork);
                                        } catch (Exception e) {
                                            LOGGER.error("Error with disconnection?", e);
                                        }

                                        if (SignalConnectionBase.this.connectionHandle == connectionHandle) {
                                            clearConnectionHandle(connectionHandle);
                                        } else {
                                            LOGGER.error(String.format("The connectionHandle was not matching. (%s/%s)", SignalConnectionBase.this.connectionHandle, connectionHandle));
                                        }
                                    }

                                    clearDisconnectFutureIfSame(finalDisconnectingFuture);

                                    if (causedByNetwork) {
                                        bindReconnectStrategy();
                                    }
                                }
                            }

                            LOGGER.debug("Notifying listeners and successing future for disconnection");
                            finalDisconnectingFuture.setSuccess(connectionHandle);
                            disconnectEvent.notifyObservers(connectionHandle, connectionHandle);
                        }
                    }
                });

                return finalDisconnectingFuture;
            }
        }
    }

    private void setConnectionHandle(ConnectionHandle connectionHandle) {
        ensureLock(CONNECTION_BEING_TOUCHED_LOCK);
        if (connectionHandle != null) {
            ensureLock(connectionHandle);
        }

        this.connectionHandle = connectionHandle;
    }

    private void clearConnectionHandle(ConnectionHandle connectionHandle) {
        ensureLock(CONNECTION_BEING_TOUCHED_LOCK);

        if (getConnectionHandle() != null) {
            Asserts.assertTrue(getConnectionHandle() == connectionHandle, "Not same.");
        }

        setConnectionHandle(null);
    }

    @Override
    public ObservableFuture<ConnectionHandle> disconnect() {
        return disconnect(false);
    }


    protected synchronized ObservableFuture<ConnectionHandle> disconnect(ConnectionHandle connectionHandle, boolean causedByNetwork) {
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
    public void ping() {
        LOGGER.debug("Keepalive requested");
        send(PingPongCommand.getShortformInstance());
    }

    @Override
    public ObservableFuture<ConnectionHandle> reconnect() {
        LOGGER.error("Issuing reconnect()");

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

    protected abstract ConnectionHandle createConnectionHandle();

    protected abstract void executeConnect(ConnectionHandle connectionHandle, SocketAddress address) throws Throwable;


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
        final ConnectionHandle c = getConnectionHandle();
        if (connectionHandle == null) {
            LOGGER.error("The connectionHandle was null, so most certainly was not active. Quitting");
            return;
        } else if (runnable == null) {
            throw new NullPointerException("The runnable can never be null.");
        } else if (connectionHandle != c) {
            LOGGER.error(String.format("The connectionHandle %s was not the same as %s. Quitting.", connectionHandle, c));
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
                            return;
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

    protected void bindReconnectStrategy() {
        ensureLock(CONNECTION_BEING_TOUCHED_LOCK);

        if (reconnectStrategy != null) {
            reconnectStrategy.start();
        }
    }

    /**
     * Returns if it was bound (ie: you unbound it)
     *
     * @return
     */
    protected synchronized boolean cancelAndUbindReconnectStrategy() {
        ensureLock(CONNECTION_BEING_TOUCHED_LOCK);

        if (reconnectStrategy != null) {
            boolean result = reconnectStrategy.isStarted();
            reconnectStrategy.stop();
            return result;
        }

        return false;
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

    public ConnectionState getConnectionState() {

        // The futures cannot change without a synchronization on "SignalConnection"

        ObservableFuture<?> cF = connectFuture;
        if (cF != null) {
            synchronized (cF) {
                if (!cF.isDone() && !cF.isCancelled() && connectFuture == cF) {
                    return ConnectionState.CONNECTING;
                }
            }
        }

        ObservableFuture<?> dF = disconnectFuture;
        if (dF != null) {
            synchronized (dF) {
                if (!dF.isDone() && !dF.isCancelled() && disconnectFuture == dF) {
                    return ConnectionState.DISCONNECTING;
                }
            }
        }

        // you cannot change the current ConnectionHandle while holding the CONNECTION_BEING_TOUCHED_LOCK
        final ConnectionHandle finalConnectionHandle = getConnectionHandle();
        if (finalConnectionHandle == null || finalConnectionHandle.isDestroyed()) {
            return ConnectionState.DISCONNECTED;
        } else {
            return ConnectionState.CONNECTED;
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

    private final Observer<ObservableFuture<ConnectionHandle>> resetConnectFutureIfActiveObserver = new Observer<ObservableFuture<ConnectionHandle>>() {
        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> future) {
            synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
                synchronized (future) {
                    clearConnectFutureIfSame(future);
                }
            }
        }
    };

    private final Observer<ObservableFuture<ConnectionHandle>> resetDisconnectFutureIfActiveObserver = new Observer<ObservableFuture<ConnectionHandle>>() {
        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> future) {
            synchronized (CONNECTION_BEING_TOUCHED_LOCK) {
                clearDisconnectFutureIfSame(future);
            }
        }
    };

    private void setDisconnectFuture(ObservableFuture<ConnectionHandle> future) {
        if (future == null) {
            throw new NullPointerException("If you want to set the future to null, use .clearDisconnectFuture() instead.");
        }

        ensureLock(CONNECTION_BEING_TOUCHED_LOCK);
        ensureLock(future);

        disconnectFuture = future;
    }

    private void setConnectFuture(ObservableFuture<ConnectionHandle> future) {
        if (future == null) {
            throw new NullPointerException("If you want to set the future to null, use .clearConnectFuture() instead.");
        }

        ensureLock(CONNECTION_BEING_TOUCHED_LOCK);
        ensureLock(future);

        connectFuture = future;
    }

    private void clearDisconnectFutureIfSame(ObservableFuture<ConnectionHandle> future) {
        ensureLock(CONNECTION_BEING_TOUCHED_LOCK);
        ensureLock(future);

        if (disconnectFuture == future) {
            clearDisconnectFuture();
        }
    }

    private void clearConnectFutureIfSame(ObservableFuture<ConnectionHandle> future) {
        ensureLock(CONNECTION_BEING_TOUCHED_LOCK);
        ensureLock(future);

        if (connectFuture == future) {
            clearConnectFuture();
        }
    }

    private void clearDisconnectFuture() {
        accessDisconnectFuture();
        changeDisconnectFuture(disconnectFuture);

        disconnectFuture = null;
    }

    private void clearConnectFuture() {
        accessConnectFuture();
        changeConnectFuture(connectFuture);

        connectFuture = null;
    }

    private void cancelDisconnectFuture() {
        accessDisconnectFuture();
        changeDisconnectFuture(disconnectFuture);

        final ObservableFuture<ConnectionHandle> dF = disconnectFuture;
        clearDisconnectFuture();
        cancelFuture(dF);
    }

    private void cancelConnectFuture() {
        accessConnectFuture();
        changeConnectFuture(connectFuture);

        ObservableFuture<?> f = connectFuture;
        clearConnectFuture();
        cancelFuture(f);
    }

    private void accessConnectFuture() {
        ensureLock(CONNECTION_BEING_TOUCHED_LOCK);
    }

    private void accessDisconnectFuture() {
        ensureLock(CONNECTION_BEING_TOUCHED_LOCK);
    }

    private void changeDisconnectFuture(ObservableFuture<ConnectionHandle> future) {
        accessDisconnectFuture();

        changeFuture(disconnectFuture, future);
    }

    private void changeConnectFuture(ObservableFuture<ConnectionHandle> future) {
        accessConnectFuture();

        changeFuture(connectFuture, future);
    }

    private <T> void changeFuture(ObservableFuture<T> future1, ObservableFuture<T> future2) {
        ensureLock(future1);
        ensureLock(future2);
        Asserts.assertTrue(future1 == null || future1 == future2, "The future must match! You have the wrong lock.");

    }

    private void cancelFuture(ObservableFuture<?> future) {
        if (future == null || future.isDone()) {
            return;
        }

        ensureLock(future);

        if (future.isDone()) {
            return;
        }
        future.cancel();
    }
}
