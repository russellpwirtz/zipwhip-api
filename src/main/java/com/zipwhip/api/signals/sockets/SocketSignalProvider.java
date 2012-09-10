package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.NestedObservableFuture;
import com.zipwhip.api.signals.*;
import com.zipwhip.api.signals.commands.*;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.concurrent.DebugObserver;
import com.zipwhip.concurrent.DifferentExecutorObserverAdapter;
import com.zipwhip.concurrent.FakeFailingObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.FakeObservableFuture;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.important.Scheduler;
import com.zipwhip.important.schedulers.HashedWheelScheduler;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.FutureDateUtil;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.zipwhip.concurrent.ThreadUtil.ensureLock;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/1/11 Time: 4:30 PM
 * <p/>
 * The SocketSignalProvider will connect to the Zipwhip SignalServer via TCP.
 * <p/>
 * This interface is intended to be used by 1 and only 1 ZipwhipClient object.
 * This is a very high level interaction where you connect for 1 user.
 */
public class SocketSignalProvider extends SignalProviderBase implements SignalProvider {

    private static final Logger LOGGER = Logger.getLogger(SocketSignalProvider.class);

    private final Map<String, SlidingWindow<Command>> slidingWindows = new HashMap<String, SlidingWindow<Command>>();

    protected ImportantTaskExecutor importantTaskExecutor;
    protected final Scheduler scheduler;

    // this is how we interact with the underlying signal server.
    protected final SignalConnection signalConnection;

    public SocketSignalProvider() {
        this(new NettySignalConnection());
    }

    public SocketSignalProvider(SignalConnection connection) {
        this(connection, null);
    }

    public SocketSignalProvider(SignalConnection connection, Executor executor) {
        this(connection, executor, null);
    }

    public SocketSignalProvider(SignalConnection connection, Executor executor, ImportantTaskExecutor importantTaskExecutor) {
        this(connection, executor, importantTaskExecutor, null);
    }

    public SocketSignalProvider(SignalConnection connection, Executor executor, ImportantTaskExecutor importantTaskExecutor, Scheduler scheduler) {
        super(executor);

        if (scheduler == null) {
            scheduler = new HashedWheelScheduler("SocketSignalProvider");
            this.link((Destroyable) scheduler);
        }
        this.scheduler = scheduler;

        // TODO: we need to double check that the connection state hasn't changed while waiting.
        scheduler.onScheduleComplete(this.onScheduleComplete);

        if (connection == null) {
            this.signalConnection = new NettySignalConnection();
            this.link(signalConnection);
        } else {
            this.signalConnection = connection;
        }

        if (importantTaskExecutor == null) {
            importantTaskExecutor = new ImportantTaskExecutor();
            this.link(importantTaskExecutor);
        }
        this.importantTaskExecutor = importantTaskExecutor;

        this.initEvents2();
    }

    private void initEvents2() {
        resetConnectFutureIfMatchingObserver = new DifferentExecutorObserverAdapter<ObservableFuture<ConnectionHandle>>(executor,
                new ThreadSafeObserverAdapter<ObservableFuture<ConnectionHandle>>(
                        resetConnectFutureIfMatchingObserver));

        notifyConnectedOnFinishObserver = new DifferentExecutorObserverAdapter<ObservableFuture<ConnectionHandle>>(executor,
                new ThreadSafeObserverAdapter<ObservableFuture<ConnectionHandle>>(
                        notifyConnectedOnFinishObserver));


        // TODO: do we really have to do this? Isn't the "importantTaskExecutor" responsible for this?
        this.signalConnection.getConnectEvent().addObserver(
                new DifferentExecutorObserverAdapter<ConnectionHandle>(executor,
                        new ThreadSafeObserverAdapter<ConnectionHandle>(sendConnectCommandIfConnectedObserver)));

        /**
         * Forward disconnect events up to clients
         */
        this.signalConnection.getDisconnectEvent().addObserver(
                new DifferentExecutorObserverAdapter<ConnectionHandle>(executor,
                        new ThreadSafeObserverAdapter<ConnectionHandle>(
                                new ActiveConnectionObserverAdapter<ConnectionHandle>(executeDisconnectStateObserver))));

        this.signalConnection.getCommandReceivedEvent().addObserver(
                new DifferentExecutorObserverAdapter<Command>(executor,
                        new ThreadSafeObserverAdapter<Command>(
                                new ActiveConnectionObserverAdapter<Command>(onMessageReceived_ON_CHANNEL_THREAD))));

        this.signalConnection.getPingEventReceivedEvent().addObserver(
                new DifferentExecutorObserverAdapter<PingEvent>(executor,
                        new ThreadSafeObserverAdapter<PingEvent>(
                                new ActiveConnectionObserverAdapter<PingEvent>(pingReceivedEvent))));

        // the ActiveConnectionObserverAdapter will filter out old noise and adapt over the "sender" to the currentConnection if/only if they are active.
        this.signalConnection.getExceptionEvent().addObserver(new ActiveConnectionObserverAdapter<String>(exceptionEvent));

        /**
         * Observe our own version changed events so we can stay in sync internally
         */
        getVersionChangedEvent().addObserver(updateVersionsOnVersionChanged);
        getNewClientIdReceivedEvent().addObserver(updateStateOnNewClientIdReceived);
    }

    private final Observer<Command> onMessageReceived_ON_CHANNEL_THREAD = new Observer<Command>() {

        /**
         * The NettySignalConnection will call this method when there's an
         * event from the remote SignalServer.
         *
         * @param sender The sender might not be the same object every time.
         * @param command Rich object representing the command received from the SignalServer.
         */
        @Override
        public void notify(Object sender, Command command) {
            SignalProviderConnectionHandle connection = (SignalProviderConnectionHandle) sender;

            Asserts.assertTrue(!connection.isDestroyed(), "The connection wasn't active?!?");

            // Check if this command has a valid version number associated with it...
            if (command.getVersion() != null && command.getVersion().getValue() > 0) {

                String versionKey = command.getVersion().getKey();

                synchronized (slidingWindows) {
                    if (!slidingWindows.containsKey(versionKey)) {

                        LOGGER.warn("Creating sliding window for key " + versionKey);

                        SlidingWindow<Command> newWindow = new SlidingWindow<Command>(versionKey);
                        newWindow.onHoleTimeout(signalHoleObserver);
                        newWindow.onPacketsReleased(packetReleasedObserver);

                        if (versions != null && versions.get(versionKey) != null) {
                            LOGGER.debug("Initializing sliding window index sequence to " + versions.get(versionKey));
                            newWindow.setIndexSequence(versions.get(versionKey));
                        }

                        slidingWindows.put(versionKey, newWindow);
                    }
                }

                // This list will be populated with the sequential packets that should be released
                List<Command> commandResults = new ArrayList<Command>();

                LOGGER.debug("Signal version " + command.getVersion().getValue());

                SlidingWindow.ReceiveResult result = slidingWindows.get(versionKey).receive(command.getVersion().getValue(), command, commandResults);

                switch (result) {
                    case EXPECTED_SEQUENCE:
                        LOGGER.debug("EXPECTED_SEQUENCE");
                        handleCommands(connection, commandResults);
                        break;
                    case HOLE_FILLED:
                        LOGGER.debug("HOLE_FILLED");
                        handleCommands(connection, commandResults);
                        break;
                    case DUPLICATE_SEQUENCE:
                        LOGGER.warn("DUPLICATE_SEQUENCE");
                        break;
                    case POSITIVE_HOLE:
                        LOGGER.warn("POSITIVE_HOLE");
                        break;
                    case NEGATIVE_HOLE:
                        LOGGER.debug("NEGATIVE_HOLE");
                        handleCommands(connection, commandResults);
                        break;
                    default:
                        LOGGER.warn("UNKNOWN_RESULT");
                }
            } else {
                // Non versioned command, not windowed
                handleCommand(connection, command);
            }
        }

        @Override
        public String toString() {
            return "onMessageReceived_ON_CHANNEL_THREAD";
        }
    };

    private final Observer<ConnectionHandle> sendConnectCommandIfConnectedObserver = new Observer<ConnectionHandle>() {

        /**
         * The NettySignalConnection will call this method when a TCP socket connection is attempted.
         */
        @Override
        public void notify(Object sender, final ConnectionHandle socketConnectionHandle) {

            ObservableFuture<ConnectionHandle> connectFuture = getUnchangingConnectFuture();
            if (connectFuture != null) {
                LOGGER.warn(String.format("We were currently 'connecting' and got a %s hit", this));
                return;
            } else if (socketConnectionHandle == null) {
                throw new NullPointerException("The socketConnectionHandle can never be null!");
            }

            synchronized (CONNECTION_HANDLE_LOCK) {
                SignalProviderConnectionHandle connectionHandle = getUnchangingConnectionHandle();

                if (connectionHandle == null) {
                    // this might be an unsolicited reconnect due to the ReconnectStrategy of the SignalConnection.
                    // In that scenario we won't see this connection coming. We need to just accept this case and self heal.
                    ensureState(ConnectionState.DISCONNECTED);

                    // we just transitioned to a connected state!
                    connectionHandle = createAndSetActiveSelfHealingSignalProviderConnection();
                    connectionHandle.setConnectionHandle(socketConnectionHandle);
                }

                final SignalProviderConnectionHandle finalSignalProviderConnectionHandle = connectionHandle;

                Asserts.assertTrue(finalSignalProviderConnectionHandle != null, "getCurrentConnectionHandle() was null!");

                if (!finalSignalProviderConnectionHandle.isFor(socketConnectionHandle)) {
                    // that's ok.
                    LOGGER.error(String.format("Got a stale request? Connection %s was not for %s", finalSignalProviderConnectionHandle, socketConnectionHandle));
                    return;
                }

                Asserts.assertTrue(finalSignalProviderConnectionHandle.isFor(socketConnectionHandle), String.format("getCurrentConnectionHandle() said this was for a different connection (%s/%s)!", socketConnectionHandle, finalSignalProviderConnectionHandle.connectionHandle));

                synchronized (socketConnectionHandle) {
                    boolean connected = !socketConnectionHandle.isDestroyed();

                    if (!connected) {
                        LOGGER.warn(String.format("Got a %s hit but the handle was destroyed: %s", this, socketConnectionHandle));
                        return;
                    }

                    // The socketConnectionHandle is not allowed to destroy during this block.
                    // The socketConnectionHandle is not allowed to destroy during this block.
                    // The socketConnectionHandle is not allowed to destroy during this block.

                    /**
                     * If we have a successful TCP connection then check if we need to send the connect command.
                     */
                    ObservableFuture<ConnectCommand> connectCommandFuture = writeConnectCommandAsyncWithTimeoutBakedIn(socketConnectionHandle);

                    connectCommandFuture.addObserver(
                            new DifferentExecutorObserverAdapter<ObservableFuture<ConnectCommand>>(executor,
                                    new ThreadSafeObserverAdapter<ObservableFuture<ConnectCommand>>(
                                            new HandleConnectCommandIfDoneObserver(finalSignalProviderConnectionHandle))));

                    connectCommandFuture.addObserver(
                            new DifferentExecutorObserverAdapter<ObservableFuture<ConnectCommand>>(executor,
                                    new ThreadSafeObserverAdapter<ObservableFuture<ConnectCommand>>(
                                            new Observer<ObservableFuture<ConnectCommand>>() {
                                                @Override
                                                public void notify(Object sender, final ObservableFuture<ConnectCommand> future) {

                                                    if (future.isSuccess()) {
                                                        finalSignalProviderConnectionHandle.finishedActionConnect = true;

                                                        if (!socketConnectionHandle.isDestroyed()) {
                                                            // great, we're still active and we got a success from the future
                                                            // transition our state.
                                                            notifyConnected(finalSignalProviderConnectionHandle, true);
                                                        } else {
                                                            // the socketConnection isn't active. Someone else torn it down or we're late to the game.
                                                            // just quit.
                                                            LOGGER.error("The socketConnectionHandle was destroyed!");
                                                        }
                                                    } else {
                                                        // we might be in a timeout scenario?
                                                        // who's job is it to kill the connection?
                                                        // we'll kill it. This shoud cause a reconnect because we passed in true
                                                        // the reconnect strategy should kick in?
                                                        LOGGER.error("Issuing disconnect request since future was not successful.");
                                                        socketConnectionHandle.disconnect(true);
                                                    }
                                                }
                                            })));
                }
            }
        }

        @Override
        public String toString() {
            return "sendConnectCommandIfConnectedObserver";
        }
    };

    private final Observer<ConnectionHandle> executeDisconnectStateObserver = new Observer<ConnectionHandle>() {

        @Override
        public void notify(Object sender, ConnectionHandle socketConnectionHandle) {
            accessConnectionHandle();

            ConnectionHandle myConnectionHandle = (ConnectionHandle) sender;

            executeDisconnect(myConnectionHandle);
        }

        @Override
        public String toString() {
            return "executeDisconnectStateObserver";
        }
    };

    private void executeDisconnect(ConnectionHandle connectionHandle) {
        synchronized (signalConnection) {
            accessConnectionHandle();

            final SignalProviderConnectionHandle finalSignalProviderConnectionHandle = this.getCurrentConnectionHandle();

            if (finalSignalProviderConnectionHandle == null) {
                LOGGER.error("Does not seem to be an active ConnectionHandle. Quitting executeDisconnect()");
                return;
            } else if (finalSignalProviderConnectionHandle != connectionHandle) {
                LOGGER.error("The connectionHandles didnt agree, so not executing the disconnect events");
                return;
            }

            synchronized (finalSignalProviderConnectionHandle) {
                // We have the SignalConnection lock so we're allowed to set this.
                clearConnectionHandle(connectionHandle);
                ((SignalProviderConnectionHandle) connectionHandle).destroy();
            }
        }

        // TODO: do we execute these observers while holding locks?
        connectionHandle.getDisconnectFuture().setSuccess(connectionHandle);
        connectionChangedEvent.notifyObservers(connectionHandle, Boolean.FALSE);
    }

    private final Observer<String> updateStateOnNewClientIdReceived = new Observer<String>() {

        @Override
        public void notify(Object sender, String newClientId) {
            SignalProviderConnectionHandle connection = (SignalProviderConnectionHandle) sender;

            Asserts.assertTrue(!connection.isDestroyed(), "bad state?");

            clientId = newClientId;
            originalClientId = newClientId;

            if (presence != null) {
                presence.setAddress(new ClientAddress(newClientId));
            }
        }

        @Override
        public String toString() {
            return "updateStateOnNewClientIdReceived";
        }
    };

    private void handleCommands(SignalProviderConnectionHandle connection, List<Command> commands) {
        for (Command command : commands) {
            handleCommand(connection, command);
        }
    }

    private final Observer<VersionMapEntry> updateVersionsOnVersionChanged = new Observer<VersionMapEntry>() {
        @Override
        public void notify(Object sender, VersionMapEntry version) {
            SignalProviderConnectionHandle connection = (SignalProviderConnectionHandle) sender;

            Asserts.assertTrue(!connection.isDestroyed(), "The connection is not active?!?");

            versions.put(version.getKey(), version.getValue());
        }

        @Override
        public String toString() {
            return "onVersionChanged";
        }
    };

    private void handleCommand(SignalProviderConnectionHandle connection, Command command) {

        commandReceivedEvent.notifyObservers(connection, command);

        if (command.getVersion() != null && command.getVersion().getValue() > 0) {
            newVersionEvent.notifyObservers(connection, command.getVersion());
        }

        if (command instanceof ConnectCommand) {

            handleConnectCommand(connection, (ConnectCommand) command);

        } else if (command instanceof DisconnectCommand) {

            handleDisconnectCommand(connection, (DisconnectCommand) command);

        } else if (command instanceof SubscriptionCompleteCommand) {

            handleSubscriptionCompleteCommand(connection, (SubscriptionCompleteCommand) command);

        } else if (command instanceof SignalCommand) {

            handleSignalCommand(connection, (SignalCommand) command);

        } else if (command instanceof PresenceCommand) {

            handlePresenceCommand(connection, (PresenceCommand) command);

        } else if (command instanceof SignalVerificationCommand) {

            handleSignalVerificationCommand(connection, (SignalVerificationCommand) command);

        } else if (command instanceof NoopCommand) {

            LOGGER.debug("Received NoopCommand");

        } else {

            LOGGER.warn("Unrecognized command: " + command.getClass().getSimpleName());
        }
    }

    /*
	 * This method allows us to decouple connection.connect() from provider.connect() for
	 * cases when we have been notified by the connection that it has a successful connection.
	 */
    private ObservableFuture<ConnectCommand> writeConnectCommandAsyncWithTimeoutBakedIn(ConnectionHandle connectionHandle) {
        return writeConnectCommandAsyncWithTimeoutBakedIn(connectionHandle, clientId, versions);
    }

    /**
     * This future will self cancel if the timeout elapses.
     */
    private ObservableFuture<ConnectCommand> writeConnectCommandAsyncWithTimeoutBakedIn(ConnectionHandle connectionHandle, String clientId, Map<String, Long> versions) {
        return importantTaskExecutor.enqueue(null,
                new ConnectCommandTask(SocketSignalProvider.this.signalConnection, connectionHandle, clientId, versions, presence),
                FutureDateUtil.inFuture(SocketSignalProvider.this.signalConnection.getConnectTimeoutSeconds(), TimeUnit.SECONDS));
    }

    private void notifyConnected(ConnectionHandle connectionHandle, boolean connected) {

        synchronized (CONNECTION_HANDLE_LOCK) {
            ensureCorrectConnectionHandle(connectionHandle);

            // If the state has changed then notify
            connectionChangedEvent.notifyObservers(connectionHandle, connected);
        }
    }

    /**
     * Won't change for the duration of your locks
     */
    public synchronized ConnectionState getUnchangingConnectionState() {
        synchronized (signalConnection) {
            accessConnectionState();
            ConnectionState signalConnectionState = signalConnection.getConnectionState();

            accessConnectionHandle();
            final SignalProviderConnectionHandle connectionHandle = getCurrentConnectionHandle();

            if (connectionHandle == null) {
                return ConnectionState.DISCONNECTED;
            }

            switch (signalConnectionState) {
                case CONNECTING:
                    return ConnectionState.CONNECTING;
                case CONNECTED:
                    if (connectionHandle.finishedActionConnect) {
                        return ConnectionState.AUTHENTICATED;
                    }

                    return ConnectionState.CONNECTED;
                case DISCONNECTING:
                    return ConnectionState.DISCONNECTING;
                case DISCONNECTED:
                    return ConnectionState.DISCONNECTED;
                default:
                    throw new IllegalStateException("Odd unexpected state");
            }
        }
    }

    public ConnectionState getConnectionState() {
        synchronized (signalConnection) {

            ConnectionState signalConnectionState = signalConnection.getConnectionState();
            final SignalProviderConnectionHandle connectionHandle = (SignalProviderConnectionHandle) getCurrentConnectionHandle();

            if (connectionHandle == null) {
                return ConnectionState.DISCONNECTED;
            }

            switch (signalConnectionState) {
                case CONNECTING:
                    return ConnectionState.CONNECTING;
                case CONNECTED:
                    if (connectionHandle.finishedActionConnect) {
                        return ConnectionState.AUTHENTICATED;
                    }

                    return ConnectionState.CONNECTED;
                case DISCONNECTING:
                    return ConnectionState.DISCONNECTING;
                case DISCONNECTED:
                    return ConnectionState.DISCONNECTED;
                default:
                    throw new IllegalStateException("Odd unexpected state");
            }
        }
    }

    public boolean isConnected() {
        ConnectionState state = getConnectionState();
        return state == ConnectionState.CONNECTED || state == ConnectionState.AUTHENTICATED;
    }


    @Override
    public synchronized ObservableFuture<ConnectionHandle> connect(String clientId, final Map<String, Long> versions, Presence presence) {

        synchronized (signalConnection) {
            final ObservableFuture<ConnectionHandle> connectFuture = getUnchangingConnectFuture();
            if (connectFuture != null) {
                return connectFuture;
            }

            // Make sure ConnectionHandler doesn't change while you work on it.
            synchronized (CONNECTION_HANDLE_LOCK) {
                final ConnectionHandle connectionHandle = getUnchangingConnectionHandle();
                if (connectionHandle != null) {
                    return new FakeObservableFuture<ConnectionHandle>(connectionHandle, connectionHandle);
                }
            }

            Asserts.assertTrue(this.getCurrentConnectionHandle() == null, "Should be no other connection");

            // keep track of the original one, so we can detect change
            final String finalClientId = clientId;
            if (StringUtil.exists(clientId)) {
                originalClientId = clientId;
            }

            // Hold onto these objects for internal reconnect attempts
            if (presence != null) {
                this.presence = presence;
            }

            if (CollectionUtil.exists(versions)) {
                this.versions = versions;
            }

            synchronized (CONNECTION_HANDLE_LOCK) {
                final SignalProviderConnectionHandle finalSignalProviderConnectionHandle = createAndSetActiveSelfHealingSignalProviderConnection();

                final NestedObservableFuture<ConnectionHandle> finalConnectFuture = createSelfHealingConnectFuture(finalSignalProviderConnectionHandle);
                synchronized (finalConnectFuture) {
                    setConnectFuture(finalConnectFuture);

                    try {
                        // This future already has an underlying timeout.
                        // this signalConnection.connect() is
                        ObservableFuture<ConnectionHandle> requestFuture = signalConnection.connect();

                        requestFuture.addObserver(
                                new DifferentExecutorObserverAdapter<ObservableFuture<ConnectionHandle>>(executor,
                                        new ThreadSafeObserverAdapter<ObservableFuture<ConnectionHandle>>(
                                                new UpdateStateOnConnectCompleteObserver(finalConnectFuture, finalSignalProviderConnectionHandle, finalClientId))));


                    } catch (Exception e) {
                        clearConnectFuture(finalConnectFuture);
                        throw new RuntimeException(e);
                    }
                }

                return finalConnectFuture;
            }
        }
    }

    private void tearDownConnection(SignalProviderConnectionHandle connectionHandle, final ObservableFuture<ConnectionHandle> connectFuture) {
        connectionHandle.destroy();
        LOGGER.error(String.format("2 Set connectionHandle to null! %s", Thread.currentThread()));
        clearConnectFuture(connectFuture);
    }

    private void sendConnectCommand(final ObservableFuture<ConnectionHandle> finalConnectFuture, final SignalProviderConnectionHandle connectionHandle, final String clientId, Map<String, Long> versions) {
        // send in the connect command (will queue up and execute in our signalProvider.executor
        // so we must be sure not to block (it's the current thread we're on right now!)).
        ObservableFuture<ConnectCommand> sendConnectCommandFuture
                = writeConnectCommandAsyncWithTimeoutBakedIn(connectionHandle, clientId, versions);

        sendConnectCommandFuture.addObserver(new HandleConnectCommandIfDoneObserver(connectionHandle));
        sendConnectCommandFuture.addObserver(new CascadeSuccessToFuture(connectionHandle, finalConnectFuture));

//        /**
//         * Because the sendConnectCommandFuture will self-timeout, we don't have to do a block/timeout
//         * of our own.
//         *
//         * Regardless of success/failure we will be in the "signalProvider" thread.
//         */
//        sendConnectCommandFuture.addObserver(new Observer<ObservableFuture<ConnectCommand>>() {
//
//            // WE ARE IN THE "SignalProvider.executor" THREAD if that executor is "simple" then
//            // we're in the connection thread or the scheduler thread.
//            @Override
//            public void notify(Object sender, ObservableFuture<ConnectCommand> future) {
//                if (finalConnectFuture.isCancelled()) {
//                    LOGGER.warn("Our connectFuture was cancelled!");
//                    return;
//                }
//
//                // the "sender" is the "task"
//                // we are in the "signalProvider.executor" thread.
//                synchronized (SocketSignalProvider.this) {
//                    if (future.isSuccess()) {
//                        try {
//                            final ConnectCommand connectCommand = future.getResult();
//
//                            // THE handleConnectCommand METHOD WILL THROW THE APPROPRIATE EVENTS.
//                            handleConnectCommand(connectionHandle, connectCommand);
//
//                            LOGGER.debug("Success finalConnectFuture!");
//                            connectionHandle.finishedActionConnect = true;
//                        } finally {
//
//                        }
//                    } else {
//                        // NOTE: what thread are we in? is this a deadlock?
//                        // who's job is it to tear down the connection?
//                        NestedObservableFuture.syncState(future, finalConnectFuture, connectionHandle);
//
//                        // TODO: what happens if the ConnectCommand times out? Who's job is it to tear down?
//                        disconnect(true);
//                    }
//                }
//            }
//
//            @Override
//            public String toString() {
//                return "sendConnectCommandFuture";
//            }
//        });
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect() {
        return disconnect(false);
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect(boolean causedByNetwork) {
        synchronized (signalConnection) {
            accessConnectionState();
            ConnectionState state = getConnectionState();
            switch (state) {
                case CONNECTING:
                    // we'll be interrupting the signalConnection, but that's ok.
                case CONNECTED:
                    final ObservableFuture<ConnectionHandle> connectFuture = getUnchangingConnectFuture();
                    synchronized (connectFuture) {
                        // we're interrupting our tasks
                        cancelConnectFuture();
                    }
                    break;
                case AUTHENTICATED:
                    // we are allowed to disconnect in this case.
                    break;
                case DISCONNECTING:
                    return getCurrentConnectionHandle().getDisconnectFuture();
                case DISCONNECTED:
                    return new FakeFailingObservableFuture<ConnectionHandle>(this, new IllegalStateException("Not currently connected"));
            }

            synchronized (CONNECTION_HANDLE_LOCK) {
                final SignalProviderConnectionHandle finalConnectionHandle = getUnchangingConnectionHandle();

                Asserts.assertTrue(finalConnectionHandle != null, "The current connectionHandle must never disagree with the stateManager!");
                if (finalConnectionHandle == null) return null; // not reachable. just here for compiler warnings.

                synchronized (finalConnectionHandle) {
                    ObservableFuture<ConnectionHandle> disconnectFuture = signalConnection.disconnect(causedByNetwork);

                    Asserts.assertTrue(disconnectFuture != null, "DisconnectFuture null?");
                    if (disconnectFuture == null) return null; // not reachable. just here for compiler warnings.

                    if (finalConnectionHandle.connectionHandle != null) {
                        // it might be null if the connection was never fully baked.
                        Asserts.assertTrue(disconnectFuture == finalConnectionHandle.connectionHandle.getDisconnectFuture(), "The different handles didnt agree?");
                    } else {
                        disconnectFuture.addObserver(new DebugObserver<ConnectionHandle>());
                        disconnectFuture.addObserver(
                                new DifferentExecutorObserverAdapter<ObservableFuture<ConnectionHandle>>(executor,
                                        new ThreadSafeObserverAdapter<ObservableFuture<ConnectionHandle>>(
                                                new Observer<ObservableFuture<ConnectionHandle>>() {
                                                    @Override
                                                    public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                                                        synchronized (CONNECTION_HANDLE_LOCK) {
                                                            final ConnectionHandle c = getUnchangingConnectionHandle();
                                                            if (c == finalConnectionHandle) {
                                                                synchronized (c) {
                                                                    clearConnectionHandle(c);
                                                                }
                                                            }
                                                        }

                                                        NestedObservableFuture.syncState(item, finalConnectionHandle.getDisconnectFuture(), finalConnectionHandle);
                                                    }
                                                })));
                    }

                    return finalConnectionHandle.getDisconnectFuture();
                }
            }
        }
    }

    private SignalProviderConnectionHandle createAndSetActiveSelfHealingSignalProviderConnection() {
        SignalProviderConnectionHandle connection = newConnectionHandle();
        synchronized (connection) {
            this.setConnectionHandle(connection);

//            /**
//             * We need this in case it does not connect successfully.
//             */
//            connection.getDisconnectFuture().addObserver(
//                    new DifferentExecutorObserverAdapter<ObservableFuture<ConnectionHandle>>(executor,
//                            new ThreadSafeObserverAdapter<ObservableFuture<ConnectionHandle>>(
//                                    new ActiveConnectionObserverAdapter<ObservableFuture<ConnectionHandle>>(
//                                            new Observer<ObservableFuture<ConnectionHandle>>() {
//                                                @Override
//                                                public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
//                                                    ConnectionHandle connectionHandle = (ConnectionHandle) sender;
//
//                                                    synchronized (SocketSignalProvider.this) {
//                                                        final ConnectionHandle finalConnectionHandle = getUnchangingConnectionHandle();
//
//                                                        synchronized (finalConnectionHandle) {
//                                                            if (finalConnectionHandle == connectionHandle) {
//                                                                executeDisconnect(finalConnectionHandle);
//                                                            }
//                                                        }
//                                                    }
//                                                }
//
//                                                @Override
//                                                public String toString() {
//                                                    return "selfHealOnConnectionDisconnect";
//                                                }
//                                            }))));
        }

        return connection;
    }

    private NestedObservableFuture<ConnectionHandle> createSelfHealingConnectFuture(ConnectionHandle connectionHandle) {
        NestedObservableFuture<ConnectionHandle> future = new NestedObservableFuture<ConnectionHandle>(connectionHandle) {
            @Override
            public String toString() {
                return "connectFuture";
            }
        };

        future.addObserver(new DebugObserver<ConnectionHandle>());
        future.addObserver(resetConnectFutureIfMatchingObserver);
        future.addObserver(notifyConnectedOnFinishObserver);

        return future;
    }

    private Observer<ObservableFuture<ConnectionHandle>> resetConnectFutureIfMatchingObserver = new Observer<ObservableFuture<ConnectionHandle>>() {
        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> future) {
            final ObservableFuture<ConnectionHandle> connectFuture = getUnchangingConnectFuture();

            synchronized (connectFuture) {
                if (future == connectFuture) {
                    clearConnectFuture(future);
                }
            }
        }
    };

    private Observer<ObservableFuture<ConnectionHandle>> notifyConnectedOnFinishObserver = new Observer<ObservableFuture<ConnectionHandle>>() {
        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> future) {
            ConnectionHandle connectionHandle = (SignalProviderConnectionHandle) sender;

            if (future.isSuccess()) {
                Asserts.assertTrue(sender == future.getResult(), "The connections should agree.");
                boolean connected = !connectionHandle.isDestroyed();

                notifyConnected(connectionHandle, connected);
            } else {
                // Do we need to throw the not connected event? It didn't change?
//                notifyConnected(connectionHandle, false);
            }
        }
    };

    // TODO: who calls this because it could be a deadlock
    // TODO: THIS METHOD HAS SERIOUS PROBLEMS AND NEEDS TESTING
    public ObservableFuture<ConnectionHandle> resetDisconnectAndConnect() {
        synchronized (signalConnection) {
            final ConnectionHandle connectionHandle = getCurrentConnectionHandle();
            if (connectionHandle != null) {
                synchronized (connectionHandle) {
                    return executeResetDisconnectAndConnect();
                }
            } else {
                return executeResetDisconnectAndConnect();
            }
        }
    }

    private ObservableFuture<ConnectionHandle> executeResetDisconnectAndConnect() {
        final String c = clientId = originalClientId = StringUtil.EMPTY_STRING;
        // TODO: I think this is a bug. The local hashmap being cleared doesnt really do anything on disk.
        versions.clear();

        synchronized (slidingWindows) {
            for (String key : slidingWindows.keySet()) {
                slidingWindows.get(key).reset();
            }
        }

        // todo: is this the right event?
        newClientIdReceivedEvent.notifyObservers(getCurrentConnectionHandle(), c);

        return signalConnection.reconnect();
    }

    @Override
    public void ping() {
        signalConnection.ping();
    }

    @Override
    public Observable<List<Signal>> getSignalReceivedEvent() {
        return signalReceivedEvent;
    }

    @Override
    public Observable<List<SignalCommand>> getSignalCommandReceivedEvent() {
        return signalCommandReceivedEvent;
    }

    @Override
    public Observable<Boolean> getConnectionChangedEvent() {
        return connectionChangedEvent;
    }

    @Override
    public Observable<String> getNewClientIdReceivedEvent() {
        return newClientIdReceivedEvent;
    }

    @Override
    public Observable<SubscriptionCompleteCommand> getSubscriptionCompleteReceivedEvent() {
        return subscriptionCompleteReceivedEvent;
    }

    @Override
    public Observable<Boolean> getPhonePresenceReceivedEvent() {
        return presenceReceivedEvent;
    }

    @Override
    public Observable<Void> getSignalVerificationReceivedEvent() {
        return signalVerificationReceivedEvent;
    }

    @Override
    public Observable<VersionMapEntry> getVersionChangedEvent() {
        return newVersionEvent;
    }

    @Override
    public Observable<PingEvent> getPingReceivedEvent() {
        return pingReceivedEvent;
    }

    @Override
    public Observable<String> getExceptionEvent() {
        return exceptionEvent;
    }

    @Override
    public Observable<Command> getCommandReceivedEvent() {
        return commandReceivedEvent;
    }

    private void handleConnectCommand(SignalProviderConnectionHandle connectionHandle, ConnectCommand command) {
        // we are in the "Channel" thread.
        // We already have the SignalConnection and CONNECTION_BEING_CHANGED locks right now.
        // it's illegal order-of-operations to synchronize on "this"

        synchronized (connectionHandle) {

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Handling ConnectCommand " + command.isSuccessful());

            boolean newClientId = false;

            if (command.isSuccessful()) {
                // copy it over for stale checking
                originalClientId = clientId;

                clientId = command.getClientId();

                if (!StringUtil.equals(clientId, originalClientId)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Received a new client id: " + clientId);
                    }

                    newClientId = true;
                }

                if (newClientId) {
                    // not the same, lets announce
                    // announce on a separate thread
                    newClientIdReceivedEvent.notifyObservers(connectionHandle, clientId);
                }

                requestBacklog(connectionHandle);
            }
        }
    }

    private void requestBacklog(SignalProviderConnectionHandle connectionHandle) {
        // we are on the SignalProvider thread, since the events all bootstrap the notify
        // our "executor." We can't trust that the connection is still connected.
        if (versions != null) {
            // kind of cheating i guess.
            for (String key : versions.keySet()) {
                connectionHandle.write(new BackfillCommand(Collections.singletonList(versions.get(key)), key))
                        .addObserver((Observer) logIfWriteFailedObserver);
            }
        }
    }

    private void handleDisconnectCommand(SignalProviderConnectionHandle connection, DisconnectCommand command) {
        synchronized (connection) {

            LOGGER.debug("Handling DisconnectCommand");

            try {
                LOGGER.debug("Disconnecting (with network=false). There should not be any auto reconnect activity now.");
                disconnect();
            } catch (Exception e) {
                LOGGER.error("Error disconnecting", e);
            }

            if (command.isBan()) {
                LOGGER.warn("BANNED by SignalServer! Those jerks!");
            }

            // If the command has not said 'ban' or 'stop'
            if (!command.isStop() || !command.isBan()) {

                String host;
                int port;

                try {
                    InetSocketAddress address = (InetSocketAddress) this.signalConnection.getAddress();
                    host = address.getHostName();
                    port = address.getPort();
                }
                catch (Exception e) {
                    LOGGER.error("Could not determine host/port: ", e);
                    return;
                }

                boolean hostChanged = false;
                if (!StringUtil.EMPTY_STRING.equals(command.getHost())) {
                    host = command.getHost();
                    hostChanged = true;
                }

                boolean portChanged = false;
                if (command.getPort() > 0) {
                    port = command.getPort();
                    portChanged = true;
                }

                if (hostChanged || portChanged) {
                    this.signalConnection.setAddress(new InetSocketAddress(host, port));
                    LOGGER.warn(String.format("We are going to connect again %d seconds from now to host: %s and port: %s", command.getReconnectDelay(), host, port));
                } else {
                    LOGGER.debug(String.format("We are going to connect again %d seconds from now", command.getReconnectDelay()));
                }

                scheduler.schedule(originalClientId, FutureDateUtil.inFuture(command.getReconnectDelay(), TimeUnit.SECONDS));
            }
        }
    }

    private final Observer<String> onScheduleComplete = new Observer<String>() {
        @Override
        public void notify(Object sender, String clientId) {
            if (!StringUtil.equals(originalClientId, clientId)) {
                // must have been for a different request.
                return;
            }

            // Clear the clientId so we will re-up on connect
            originalClientId = StringUtil.EMPTY_STRING;

            LOGGER.debug("Executing the connect that was requested by the server. Nulled out the clientId...");
            try {
                connect();
                // TODO: what if this never finishes? Will the reconnectStrategy pay off?
            } catch (Exception e) {
                LOGGER.error("Crash on connect. We hope that the reconnectStrategy will do us good.", e);
            }
        }
    };

    private void handlePresenceCommand(SignalProviderConnectionHandle connection, PresenceCommand command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Handling PresenceCommand " + command.getPresence());
        }

        boolean selfPresenceExists = false;
        List<Presence> presenceList = command.getPresence();

        if (presenceList == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Nothing is known about us or our peers");
            }

            selfPresenceExists = false;

        } else {

            for (Presence presence : command.getPresence()) {
                if (clientId.equals(presence.getAddress().getClientId())) {
                    selfPresenceExists = true;
                }

                if (presence.getCategory().equals(PresenceCategory.Phone)) {
                    presenceReceivedEvent.notifyObservers(this, presence.getConnected());
                }
            }
        }
        if (!selfPresenceExists) {

            if (presence != null) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Reidentifying our presence object");
                }

                connection.write(new PresenceCommand(Collections.singletonList(presence)));

            } else {
                LOGGER.debug("Our presence object was empty, so we didn't share it");
            }
        }
    }

    private void handleSignalCommand(SignalProviderConnectionHandle connection, SignalCommand command) {
        LOGGER.debug("Handling SignalCommand");

        // Distribute the command and the raw signal to give client's flexibility regarding what data they need
        signalCommandReceivedEvent.notifyObservers(this, Collections.singletonList(command));
        signalReceivedEvent.notifyObservers(this, Collections.singletonList(command.getSignal()));
    }

    private void handleSubscriptionCompleteCommand(SignalProviderConnectionHandle connection, SubscriptionCompleteCommand command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Handling SubscriptionCompleteCommand " + command.toString());
        }

        if (!sendPresence(presence)) {
            LOGGER.warn("Tried and failed to send presence");
        }

//        Asserts.assertTrue(authenticationKeyChain.isAuthenticated(clientId, command.getSubscriptionId()), "This subscriptionId was already authenticated!");
//        // WARNING: We don't know which clientId this really came in for..
//        authenticationKeyChain.add(clientId, command.getSubscriptionId());

        subscriptionCompleteReceivedEvent.notifyObservers(this, command);
    }

    private boolean sendPresence(Presence presence) {
        if (presence != null) {
            // Set our clientId in case its not already there
            presence.setAddress(new ClientAddress(clientId));

            // TODO handle send future
            signalConnection.send(new PresenceCommand(Collections.singletonList(presence)));
            return true;

        } else {
            return false;
        }
    }

    private void handleSignalVerificationCommand(SignalProviderConnectionHandle connection, SignalVerificationCommand command) {
        LOGGER.debug("Processing SignalVerificationCommand " + command.toString());
        signalVerificationReceivedEvent.notifyObservers(this, null);
    }

    private final Observer<SlidingWindow.HoleRange> signalHoleObserver = new Observer<SlidingWindow.HoleRange>() {
        @Override
        public void notify(Object sender, SlidingWindow.HoleRange hole) {
            LOGGER.debug("Signal hole detected, requesting backfill for  " + hole.toString());
            signalConnection.send(new BackfillCommand(hole.getRange(), hole.key));
        }
    };

    private final Observer<List<Command>> packetReleasedObserver = new Observer<List<Command>>() {
        @Override
        public void notify(Object sender, List<Command> commands) {
            LOGGER.warn(commands.size() + " packets released due to timeout, leaving a hole.");
            // TODO: how do we know this is the right connection???
            handleCommands((SignalProviderConnectionHandle) getCurrentConnectionHandle(), commands);
        }
    };

    public ImportantTaskExecutor getImportantTaskExecutor() {
        return importantTaskExecutor;
    }

    public void setImportantTaskExecutor(ImportantTaskExecutor importantTaskExecutor) {
        this.importantTaskExecutor = importantTaskExecutor;
    }

//    private static class StateBundle {
//        private long connectionId;
//        private String clientId;
//    }

    private final Observer<ObservableFuture> logIfWriteFailedObserver = new Observer<ObservableFuture>() {
        @Override
        public void notify(Object sender, ObservableFuture item) {
            if (!item.isSuccess()) {
                LOGGER.fatal("FAILED TO WRITE TO CHANNEL! " + item);
            }
        }
    };

    @Override
    protected void accessConnectFuture() {
        ensureLock(signalConnection);
        super.accessConnectFuture();
    }

    @Override
    protected void accessConnectionHandle() {
        ensureLock(signalConnection);
        super.accessConnectionHandle();
    }

    @Override
    protected void clearConnectionHandle(ConnectionHandle finalConnectionHandle) {
        ensureLock(signalConnection);
        super.clearConnectionHandle(finalConnectionHandle);
    }

    private void ensureState(ConnectionState state) {
        accessConnectionState();

        final ConnectionState existingState = getConnectionState();

        Asserts.assertTrue(existingState == state, String.format("The state was supposed to be %s but was %s", state, existingState));
    }

    private void accessConnectionState() {
        ensureLock(SocketSignalProvider.this);
        ensureLock(signalConnection);
    }

    @Override
    protected void onDestroy() {

    }

    /**
     * This class is attached to the signalConnection.connect() future. It will execute in our executor in a
     * thread-safe way.
     */
    private class UpdateStateOnConnectCompleteObserver implements Observer<ObservableFuture<ConnectionHandle>> {

        final ObservableFuture<ConnectionHandle> finalConnectFuture;
        final SignalProviderConnectionHandle finalSignalProviderConnectionHandle;
        final String finalClientId;

        private UpdateStateOnConnectCompleteObserver(ObservableFuture<ConnectionHandle> finalConnectFuture, SignalProviderConnectionHandle finalSignalProviderConnectionHandle, String finalClientId) {
            this.finalConnectFuture = finalConnectFuture;
            this.finalSignalProviderConnectionHandle = finalSignalProviderConnectionHandle;
            this.finalClientId = finalClientId;
        }

        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> signalConnectionFuture) {
            synchronized (finalConnectFuture) {
                if (finalConnectFuture.isCancelled()) {
                    synchronized (finalSignalProviderConnectionHandle) {
                        tearDownConnection(finalSignalProviderConnectionHandle, finalConnectFuture);
                        // oh shit the returned future was cancelled!
                        Asserts.assertTrue(getConnectFuture() != finalConnectFuture, "The futures shouldn't be the same!");
                        return;
                    }
                }

                final ConnectionHandle c = getCurrentConnectionHandle();
                if (signalConnectionFuture.isCancelled()) {
                    tearDownConnection(finalSignalProviderConnectionHandle, finalConnectFuture);
                    LOGGER.warn("Our connection was cancelled. " + signalConnectionFuture);
                    finalConnectFuture.setFailure(new Exception(String.format("The connectionHandles didn't match up. Was %s expected %s", c, finalSignalProviderConnectionHandle)));
                    return;
                } else if (signalConnectionFuture.isFailed()) {
                    tearDownConnection(finalSignalProviderConnectionHandle, finalConnectFuture);
                    LOGGER.warn("Our connection was failed. " + signalConnectionFuture);
                    finalConnectFuture.setFailure(signalConnectionFuture.getCause());
                    return;
                } else if (finalSignalProviderConnectionHandle != c) {
                    tearDownConnection(finalSignalProviderConnectionHandle, finalConnectFuture);
                    LOGGER.error(String.format("The connectionHandles didn't match up. Was %s expected %s", c, finalSignalProviderConnectionHandle));
                    finalConnectFuture.setFailure(new Exception(String.format("The connectionHandles didn't match up. Was %s expected %s", c, finalSignalProviderConnectionHandle)));
                    return;
                }

                if (!signalConnectionFuture.isSuccess()) {
                    try {
                        ensureState(ConnectionState.DISCONNECTED);

                        finalSignalProviderConnectionHandle.destroy();
                        LOGGER.error(String.format("2 Set connectionHandle to null! %s", Thread.currentThread()));
                        clearConnectionHandle(finalSignalProviderConnectionHandle);
                        clearConnectFuture(finalConnectFuture);
                    } finally {
                        finalConnectFuture.setFailure(new Exception("Couldn't connect!"));
                    }
                    return;
                }

                try {
                    ensureState(ConnectionState.CONNECTED);
                } catch (Exception e) {
                    finalConnectFuture.setFailure(e);
                    throw new RuntimeException(e);
                }

                synchronized (finalSignalProviderConnectionHandle) {
                    final ConnectionHandle connectionHandle = signalConnectionFuture.getResult();

                    // set the currently active connection's internal connection.
                    finalSignalProviderConnectionHandle.setConnectionHandle(connectionHandle);

                    sendConnectCommand(finalConnectFuture, finalSignalProviderConnectionHandle, finalClientId, versions);
                }
            }
        }
    }

    private class CascadeSuccessToFuture implements Observer<ObservableFuture<ConnectCommand>> {

        final ConnectionHandle connectionHandle;
        final ObservableFuture<ConnectionHandle> future;

        private CascadeSuccessToFuture(ConnectionHandle connectionHandle, ObservableFuture<ConnectionHandle> future) {
            this.connectionHandle = connectionHandle;
            this.future = future;
        }

        @Override
        public void notify(Object sender, ObservableFuture<ConnectCommand> future) {
            synchronized (signalConnection) {
                if (future.isSuccess()) {
                    this.future.setSuccess(connectionHandle);
                } else {
                    NestedObservableFuture.syncState(future, this.future, connectionHandle);
                }
            }
        }
    }

    private class HandleConnectCommandIfDoneObserver implements Observer<ObservableFuture<ConnectCommand>> {

        private final SignalProviderConnectionHandle finalConnectionHandle;

        private HandleConnectCommandIfDoneObserver(SignalProviderConnectionHandle finalConnectionHandle) {
            this.finalConnectionHandle = finalConnectionHandle;
        }

        @Override
        public void notify(Object sender, ObservableFuture<ConnectCommand> future) {
            if (!future.isSuccess()) {
                // shit!
                return;
            }

            ConnectCommand connectCommand = future.getResult();

            finalConnectionHandle.finishedActionConnect = true;

            handleConnectCommand(finalConnectionHandle, connectCommand);
        }
    }

    private class ThreadSafeObserverAdapter<T> implements Observer<T> {

        final Observer<T> observer;

        private ThreadSafeObserverAdapter(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public void notify(Object sender, T item) {
            synchronized (SocketSignalProvider.this) {
                synchronized (signalConnection) {
                    observer.notify(sender, item);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("[t: %s]", observer.toString());
        }
    }

    private class ActiveConnectionObserverAdapter<T> implements Observer<T> {

        private final Observer<T> observer;

        private ActiveConnectionObserverAdapter(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public void notify(Object sender, T data) {
            ConnectionHandle connectionHandle = (ConnectionHandle) sender;

            synchronized (CONNECTION_HANDLE_LOCK) {
                final SignalProviderConnectionHandle signalProviderConnectionHandle = getUnchangingConnectionHandle();
                if (signalProviderConnectionHandle == null) {
                    LOGGER.error(String.format("%s: The signalProviderConnection is null, so it must be inactive. Quitting.", this));
                    return;
                }

                synchronized (signalProviderConnectionHandle) {
                    if (signalProviderConnectionHandle.isDestroyed()) {
                        LOGGER.error(String.format("%s: The signalProviderConnection is not active. Quitting.", this));
                        return;
                    } else if (!signalProviderConnectionHandle.isFor(connectionHandle)) {
                        LOGGER.error(String.format("%s: The signalProviderConnection is not for the current connection. Quitting", this));
                        return;
                    }

                    // during this observer.notify call you are guaranteed that the currentConnectionHandle cannot change
                    // underneath you.
                    observer.notify(signalProviderConnectionHandle, data);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("[a: %s]", observer);
        }
    }
}

