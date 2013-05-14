package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.api.signals.commands.*;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.concurrent.*;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.NamedThreadFactory;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.important.Scheduler;
import com.zipwhip.important.schedulers.ZipwhipTimerScheduler;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.timers.HashedWheelTimer;
import com.zipwhip.timers.Timer;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.FutureDateUtil;
import com.zipwhip.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketSignalProvider.class);

    private final Map<String, SlidingWindow<Command>> slidingWindows = new HashMap<String, SlidingWindow<Command>>();

    protected final ImportantTaskExecutor importantTaskExecutor;
    protected final Scheduler scheduler;
    protected final Timer timer;

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

    public SocketSignalProvider(SignalConnection connection, Executor executor, Timer timer) {
        super(executor);

        if (timer == null) {
            this.timer = new HashedWheelTimer(new NamedThreadFactory("SocketSignalProvider-"), 1, TimeUnit.SECONDS);
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    SocketSignalProvider.this.timer.stop();
                }
            });
        } else {
            this.timer = timer;
        }

        this.scheduler = new ZipwhipTimerScheduler(this.timer);
        this.importantTaskExecutor = new ImportantTaskExecutor(this.scheduler);
        this.link(importantTaskExecutor);

        // TODO: we need to double check that the connection state hasn't changed while waiting.
        this.scheduler.onScheduleComplete(this.onScheduleComplete);

        if (connection == null) {
            this.signalConnection = new NettySignalConnection();
            this.link(signalConnection);
        } else {
            this.signalConnection = connection;
        }

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
                                executeDisconnectStateObserver)));

        this.signalConnection.getCommandReceivedEvent().addObserver(
                new DifferentExecutorObserverAdapter<Command>(executor,
                        new ThreadSafeObserverAdapter<Command>(
                                new ActiveConnectionObserverAdapter<Command>(onMessageReceived))));

        this.signalConnection.getPingEventReceivedEvent().addObserver(
                new DifferentExecutorObserverAdapter<PingEvent>(executor,
                        new ThreadSafeObserverAdapter<PingEvent>(
                                new ActiveConnectionObserverAdapter<PingEvent>(pingReceivedEvent))));

        // the ActiveConnectionObserverAdapter will filter out old noise and adapt over the "sender" to the currentConnection if/only if they are active.
        this.signalConnection.getExceptionEvent().addObserver(
                new DifferentExecutorObserverAdapter<String>(executor,
                        new ThreadSafeObserverAdapter<String>(
                                new ActiveConnectionObserverAdapter<String>(exceptionEvent))));

        /**
         * Observe our own version changed events so we can stay in sync internally
         */
        getVersionChangedEvent().addObserver(updateVersionsOnVersionChanged);
        getNewClientIdReceivedEvent().addObserver(updateStateOnNewClientIdReceived);
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> connect(String clientId, final Map<String, Long> versions, Presence presence) {

        synchronized (signalConnection) {
            final ObservableFuture<ConnectionHandle> connectFuture = getUnchangingConnectFuture();
            if (connectFuture != null) {
                return connectFuture;
            }

            // Make sure ConnectionHandler doesn't change while you work on it.
            synchronized (PROVIDER_CONNECTION_HANDLE_LOCK) {
                final ConnectionHandle connectionHandle = getUnchangingConnectionHandle();
                if (connectionHandle != null) {
                    LOGGER.warn(String.format("We are already connected (%s). Returning fake future.", connectionHandle));
                    return new FakeObservableFuture<ConnectionHandle>(connectionHandle, connectionHandle);
                }

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

                final SignalProviderConnectionHandle finalSignalProviderConnectionHandle = createAndSetActiveSignalProviderConnection();

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
                        clearConnectionHandle(finalSignalProviderConnectionHandle);
                        clearConnectFuture(finalConnectFuture);
                        throw new RuntimeException(e);
                    }
                }

                return finalConnectFuture;
            }
        }
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect() {
        return disconnect(false);
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect(boolean causedByNetwork) {
        synchronized (signalConnection) {
            synchronized (PROVIDER_CONNECTION_HANDLE_LOCK) {
                accessConnectionState();
                accessConnectionHandle();

                ConnectionState state = getConnectionState();
                switch (state) {
                    case CONNECTING:
                        // we'll be interrupting the signalConnection, but that's ok.
                    case CONNECTED:
                        final ObservableFuture<ConnectionHandle> connectFuture = getUnchangingConnectFuture();
                        if (connectFuture != null){
                            synchronized (connectFuture) {
                                // we're interrupting our tasks
                                cancelConnectFuture();
                            }
                        }
                        break;
                    case AUTHENTICATED:
                        // we are allowed to disconnect in this case.
                        break;
                    case DISCONNECTING:
                        return getUnchangingConnectionHandle().getDisconnectFuture();
                    case DISCONNECTED:
                        return new FakeFailingObservableFuture<ConnectionHandle>(this, new IllegalStateException("Not currently connected"));
                }

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
                                                        synchronized (PROVIDER_CONNECTION_HANDLE_LOCK) {
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

    private final Observer<Command> onMessageReceived = new Observer<Command>() {

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

                        SlidingWindow<Command> newWindow = new SlidingWindow<Command>(timer, versionKey);
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
                        LOGGER.debug("EXPECTED_SEQUENCE: " + commandResults);
                        handleCommands(connection, commandResults);
                        break;
                    case HOLE_FILLED:
                        LOGGER.debug("HOLE_FILLED: " + commandResults);
                        handleCommands(connection, commandResults);
                        break;
                    case DUPLICATE_SEQUENCE:
                        LOGGER.warn("DUPLICATE_SEQUENCE: " + commandResults);
                        break;
                    case POSITIVE_HOLE:
                        LOGGER.warn("POSITIVE_HOLE: " + commandResults);
                        break;
                    case NEGATIVE_HOLE:
                        LOGGER.debug("NEGATIVE_HOLE: " + commandResults);
                        handleCommands(connection, commandResults);
                        break;
                    default:
                        LOGGER.warn("UNKNOWN_RESULT: " + commandResults);
                }
            } else {
                // Non versioned command, not windowed
                handleCommand(connection, command);
            }
        }

        @Override
        public String toString() {
            return "onMessageReceived";
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
                LOGGER.warn(String.format("We were currently 'connecting' and got a %s hit. Just quitting.", this));
                return;
            } else if (socketConnectionHandle == null) {
                throw new NullPointerException("The socketConnectionHandle can never be null!");
            }

            synchronized (PROVIDER_CONNECTION_HANDLE_LOCK) {
                SignalProviderConnectionHandle connectionHandle = getUnchangingConnectionHandle();

                if (connectionHandle == null) {
                    // this might be an unsolicited reconnect due to the ReconnectStrategy of the SignalConnection.
                    // In that scenario we won't see this connection coming. We need to just accept this case and self heal.
                    ensureState(ConnectionState.DISCONNECTED);

                    // we just transitioned to a connected state!
                    connectionHandle = createAndSetActiveSignalProviderConnection();
                    connectionHandle.setConnectionHandle(socketConnectionHandle);
                    LOGGER.debug(String.format("Created our own in-line connectionHandle %s", connectionHandle));
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
            ObservableFuture<ConnectionHandle> connectionFuture = getUnchangingConnectFuture();
            if (connectionFuture != null && !connectionFuture.isDone()) {
                LOGGER.debug("executeDisconnectStateObserver: Currently in the connecting phase, ignoring notify()");
                return;
            }

            synchronized (PROVIDER_CONNECTION_HANDLE_LOCK) {
                accessConnectionHandle();

                SignalProviderConnectionHandle myConnectionHandle = getUnchangingConnectionHandle();
                if (myConnectionHandle == null || myConnectionHandle.isDestroyed()) {
                    LOGGER.error(String.format("myConnectionHandle was null (or destroyed)! Quitting."));
                    return;
                } else if (!myConnectionHandle.isFor(socketConnectionHandle)) {
                    LOGGER.error(String.format("executeDisconnectStateObserver got %s was expecting %s.for(). Quitting.", socketConnectionHandle, myConnectionHandle));
                    return;
                }

                executeDisconnect(myConnectionHandle);
            }
        }

        @Override
        public String toString() {
            return "executeDisconnectStateObserver";
        }
    };

    private void executeDisconnect(ConnectionHandle connectionHandle) {
        LOGGER.debug("SignalConnection said disconnected, we're going to update our local state.");

        final SignalProviderConnectionHandle finalSignalProviderConnectionHandle = this.getUnchangingConnectionHandle();

        if (finalSignalProviderConnectionHandle == null) {
            LOGGER.error("Does not seem to be an active ConnectionHandle. Quitting executeDisconnect()");
            return;
        } else if (finalSignalProviderConnectionHandle != connectionHandle) {
            LOGGER.error("The connectionHandles didnt agree, so not executing the disconnect events");
            return;
        }

        synchronized (finalSignalProviderConnectionHandle) {
            // We have the SignalConnection lock so we're allowed to set this.
            finalSignalProviderConnectionHandle.destroy();
            clearConnectionHandle(finalSignalProviderConnectionHandle);
        }

        LOGGER.debug("Announcing connection changed (successing the 'disconnectFuture')");
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

        synchronized (PROVIDER_CONNECTION_HANDLE_LOCK) {
            ensureCorrectConnectionHandle(connectionHandle);

            // If the state has changed then notify
            connectionChangedEvent.notifyObservers(connectionHandle, connected);
        }
    }

    public synchronized ConnectionState getConnectionState() {
        synchronized (signalConnection) {

            ConnectionState signalConnectionState = signalConnection.getConnectionState();
            synchronized (PROVIDER_CONNECTION_HANDLE_LOCK) {
                final SignalProviderConnectionHandle connectionHandle = getUnchangingConnectionHandle();
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
    }

    public boolean isConnected() {
        ConnectionState state = getConnectionState();
        return state == ConnectionState.CONNECTED || state == ConnectionState.AUTHENTICATED;
    }

    private void tearDownConnection(SignalProviderConnectionHandle connectionHandle, final ObservableFuture<ConnectionHandle> connectFuture) {
        connectionHandle.destroy();

        final ConnectionHandle finalConnectionHandle = getUnchangingConnectionHandle();
        if (connectionHandle != finalConnectionHandle) {
            // not the same, just shrug it off.
            LOGGER.error("The connectionHandle %s did not match %s so not doing a clear.");
            return;
        } else {
            synchronized (connectionHandle) {
                LOGGER.debug(String.format("2 Set connectionHandle to null! %s", Thread.currentThread()));
                clearConnectionHandle(connectionHandle);
            }
        }

        if (connectFuture != null) {
            synchronized (connectFuture) {
                clearConnectFuture(connectFuture);
            }
        }

        // announce we destroyed it.
        connectionHandle.getDisconnectFuture().setSuccess(connectionHandle);
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

    private SignalProviderConnectionHandle createAndSetActiveSignalProviderConnection() {
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
            if (connectFuture == null) {
                return;
            }

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
            synchronized (PROVIDER_CONNECTION_HANDLE_LOCK) {
                final ConnectionHandle connectionHandle = getUnchangingConnectionHandle();
                if (connectionHandle != null) {
                    synchronized (connectionHandle) {
                        return executeResetDisconnectAndConnect();
                    }
                } else {
                    return executeResetDisconnectAndConnect();
                }
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
        newClientIdReceivedEvent.notifyObservers(getUnchangingConnectionHandle(), c);

        return signalConnection.reconnect();
    }

    @Override
    public ObservableFuture<Boolean> ping() {
        if (signalConnection.getConnectionState() == ConnectionState.CONNECTED) {
            return signalConnection.ping();
        } else {
            return new FakeFailingObservableFuture<Boolean>(this, new Exception("Not connected"));
        }
    }


    public SignalConnection getSignalConnection() {
        return signalConnection;
    }

    private void handleConnectCommand(SignalProviderConnectionHandle connectionHandle, ConnectCommand command) {
        // we are in the "Channel" thread.
        // We already have the SignalConnection and CONNECTION_BEING_CHANGED locks right now.
        // it's illegal order-of-operations to synchronize on "this"

        synchronized (connectionHandle) {

            if (LOGGER.isDebugEnabled())
                LOGGER.debug(String.format("handleConnectCommand(%s, %s)", connectionHandle, command));

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

            // If the command has not said 'ban' and 'stop'
            if (!command.isStop() && !command.isBan()) {
                String host;
                int port;

                try {
                    InetSocketAddress address = (InetSocketAddress) this.signalConnection.getAddress();
                    host = address.getHostName();
                    port = address.getPort();
                } catch (Exception e) {
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

                scheduler.schedule(clientId, FutureDateUtil.inFuture(command.getReconnectDelay(), TimeUnit.SECONDS));
            }
        }
    }

    private final Observer<String> onScheduleComplete = new Observer<String>() {
        @Override
        public void notify(Object sender, String clientId) {
            if (!StringUtil.equals(SocketSignalProvider.this.clientId, clientId)) {
                // must have been for a different request.
                return;
            } else if (getConnectionState() == ConnectionState.CONNECTED) {
                LOGGER.debug("It seems that the connectionState is connected already. Aborting this reconnect attempt.");
                return;
            }

            LOGGER.debug("Executing the connect that was requested by the server.");
            try {
                connect(clientId, versions, presence).addObserver(retryOnFailureObserver);
            } catch (Exception e) {
                LOGGER.error("Crash on connect. We hope that the reconnectStrategy will do us good.", e);
            }
        }
    };

    private Observer<ObservableFuture<ConnectionHandle>> retryOnFailureObserver = new Observer<ObservableFuture<ConnectionHandle>>() {
        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
            if (item.isSuccess() && item.getResult() != null && !item.getResult().getDisconnectFuture().isDone()) {
                LOGGER.warn("The future was successful! We reconnected just fine.");
                return;
            } else if (item.isCancelled()) {
                LOGGER.warn("The future was cancelled, so this must mean it was forcibly disconnected! Not retrying.");
                return;
            }

            // TODO: how do we handle this case? It's considered an 'initial connect' so it can't be retried.

            connect(clientId, versions, presence).addObserver(this);
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
                    presenceReceivedEvent.notifyObservers(connection, presence.getConnected());
                }
            }
        }
        if (!selfPresenceExists) {

            if (presence != null) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Reidentifying our presence object");
                }

//                connection.write(new PresenceCommand(Collections.singletonList(presence)));
            } else {
                LOGGER.debug("Our presence object was empty, so we didn't share it");
            }
        }
    }

    private void handleSignalCommand(SignalProviderConnectionHandle connection, SignalCommand command) {
        LOGGER.debug("Handling SignalCommand");

        // Distribute the command and the raw signal to give client's flexibility regarding what data they need
        signalCommandReceivedEvent.notifyObservers(connection, Collections.singletonList(command));
        signalReceivedEvent.notifyObservers(connection, Collections.singletonList(command.getSignal()));
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
            signalConnection.send(new BackfillCommand(hole.getRange(), hole.getKey()));
        }
    };

    private final Observer<List<Command>> packetReleasedObserver = new Observer<List<Command>>() {
        @Override
        public void notify(Object sender, List<Command> commands) {
            LOGGER.warn(commands.size() + " packets released due to timeout, leaving a hole.");
            // TODO: how do we know this is the right connection???
            synchronized (SocketSignalProvider.this) {
                synchronized (signalConnection) {
                    synchronized (PROVIDER_CONNECTION_HANDLE_LOCK) {
                        final SignalProviderConnectionHandle finalConnectionHandle = getUnchangingConnectionHandle();
                        handleCommands(finalConnectionHandle, commands);
                    }
                }
            }
        }
    };

    public ImportantTaskExecutor getImportantTaskExecutor() {
        return importantTaskExecutor;
    }

//    private static class StateBundle {
//        private long connectionId;
//        private String clientId;
//    }

    private final Observer<ObservableFuture> logIfWriteFailedObserver = new Observer<ObservableFuture>() {
        @Override
        public void notify(Object sender, ObservableFuture item) {
            if (!item.isSuccess()) {
                LOGGER.error("FAILED TO WRITE TO CHANNEL! " + item);
            }
        }
    };

    @Override
    protected synchronized ObservableFuture<ConnectionHandle> disconnect(ConnectionHandle connectionHandle, boolean causedByNetwork) {
        synchronized (signalConnection) {
            return super.disconnect(connectionHandle, causedByNetwork);
        }
    }

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
            synchronized (PROVIDER_CONNECTION_HANDLE_LOCK) {
                synchronized (finalConnectFuture) {
                    if (finalConnectFuture.isCancelled()) {
                        synchronized (finalSignalProviderConnectionHandle) {
                            LOGGER.warn("Our connection was cancelled. " + finalConnectFuture);
                            tearDownConnection(finalSignalProviderConnectionHandle, finalConnectFuture);
                            // oh shit the returned future was cancelled!
                            Asserts.assertTrue(getConnectFuture() != finalConnectFuture, "The futures shouldn't be the same!");
                            return;
                        }
                    }

                    final ConnectionHandle c = getUnchangingConnectionHandle();
                    synchronized (finalSignalProviderConnectionHandle) {
                        synchronized (c) {
                            if (signalConnectionFuture.isCancelled()) {
                                LOGGER.warn("Our connection was cancelled. " + signalConnectionFuture);
                                tearDownConnection(finalSignalProviderConnectionHandle, finalConnectFuture);
                                finalConnectFuture.setFailure(new Exception(String.format("The connectionHandles didn't match up. Was %s expected %s", c, finalSignalProviderConnectionHandle)));
                                return;
                            } else if (signalConnectionFuture.isFailed()) {
                                LOGGER.warn("Our connection was failed. " + signalConnectionFuture);
                                tearDownConnection(finalSignalProviderConnectionHandle, finalConnectFuture);
                                finalConnectFuture.setFailure(signalConnectionFuture.getCause());
                                return;
                            } else if (finalSignalProviderConnectionHandle != c) {
                                tearDownConnection(finalSignalProviderConnectionHandle, finalConnectFuture);
                                LOGGER.error(String.format("The connectionHandles didn't match up. Was %s expected %s", c, finalSignalProviderConnectionHandle));
                                finalConnectFuture.setFailure(new Exception(String.format("The connectionHandles didn't match up. Was %s expected %s", c, finalSignalProviderConnectionHandle)));
                                return;
                            }
                        }

                        final ConnectionHandle connectionHandle = signalConnectionFuture.getResult();

                        // set the currently active connection's internal connection.
                        finalSignalProviderConnectionHandle.setConnectionHandle(connectionHandle);

                        sendConnectCommand(finalConnectFuture, finalSignalProviderConnectionHandle, finalClientId, versions);
                    }
                }
            }
        }
    }

    private static class CascadeSuccessToFuture implements Observer<ObservableFuture<ConnectCommand>> {

        final ConnectionHandle connectionHandle;
        final ObservableFuture<ConnectionHandle> future;

        private CascadeSuccessToFuture(ConnectionHandle connectionHandle, ObservableFuture<ConnectionHandle> future) {
            this.connectionHandle = connectionHandle;
            this.future = future;
        }

        @Override
        public void notify(Object sender, ObservableFuture<ConnectCommand> otherFuture) {
//            synchronized (signalConnection) {
            if (otherFuture.isSuccess()) {
                this.future.setSuccess(connectionHandle);
            } else {
                NestedObservableFuture.syncState(otherFuture, this.future, connectionHandle);
            }
//            }
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

            synchronized (PROVIDER_CONNECTION_HANDLE_LOCK) {
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

