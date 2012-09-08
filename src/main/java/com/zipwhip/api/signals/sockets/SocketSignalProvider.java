package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.NestedObservableFuture;
import com.zipwhip.api.signals.*;
import com.zipwhip.api.signals.commands.*;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.concurrent.FakeFailingObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.important.Scheduler;
import com.zipwhip.important.schedulers.HashedWheelScheduler;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.util.*;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/1/11 Time: 4:30 PM
 * <p/>
 * The SocketSignalProvider will connect to the Zipwhip SignalServer via TCP.
 * <p/>
 * This interface is intended to be used by 1 and only 1 ZipwhipClient object.
 * This is a very high level interaction where you connect for 1 user.
 */
public class SocketSignalProvider extends CascadingDestroyableBase implements SignalProvider {

    private static final Logger LOGGER = Logger.getLogger(SocketSignalProvider.class);
    private static AtomicLong ID = new AtomicLong();

    private final ObservableHelper<PingEvent> pingReceivedEvent = new ObservableHelper<PingEvent>("pingReceivedEvent");
    private final ObservableHelper<List<Signal>> signalReceivedEvent = new ObservableHelper<List<Signal>>("signalReceivedEvent");
    private final ObservableHelper<List<SignalCommand>> signalCommandReceivedEvent = new ObservableHelper<List<SignalCommand>>("signalCommandReceivedEvent");
    private final ObservableHelper<Void> signalVerificationReceivedEvent = new ObservableHelper<Void>("signalVerificationReceivedEvent");
    private final ObservableHelper<Command> commandReceivedEvent = new ObservableHelper<Command>("commandReceivedEvent");
    private final ObservableHelper<Boolean> presenceReceivedEvent = new ObservableHelper<Boolean>("presenceReceivedEvent");
    private final ObservableHelper<String> newClientIdReceivedEvent = new ObservableHelper<String>("newClientIdReceivedEvent");

    private final ObservableHelper<Boolean> connectionChangedEvent = new ObservableHelper<Boolean>("connectionChangedEvent");
    private final ObservableHelper<String> exceptionEvent = new ObservableHelper<String>("exceptionEvent");
    private final ObservableHelper<VersionMapEntry> newVersionEvent = new ObservableHelper<VersionMapEntry>("newVersionEvent");
    private final ObservableHelper<SubscriptionCompleteCommand> subscriptionCompleteReceivedEvent = new ObservableHelper<SubscriptionCompleteCommand>("subscriptionCompleteReceivedEvent");

    // this is how we interact with the underlying signal server.
    protected final SignalConnection signalConnection;

    protected final Scheduler scheduler;

    private ImportantTaskExecutor importantTaskExecutor;

    private Executor executor;

    private String clientId;
    private String originalClientId; //So we can detect change

    private Presence presence;
    private Map<String, Long> versions = new HashMap<String, Long>();
    private final Map<String, SlidingWindow<Command>> slidingWindows = new HashMap<String, SlidingWindow<Command>>();

    private ObservableFuture<ConnectionHandle> connectingFuture;
    private SignalProviderConnectionHandle currentSignalProviderConnection;

    public SocketSignalProvider() {
        this(new NettySignalConnection());
    }

    public SocketSignalProvider(SignalConnection conn) {
        this(conn, null, null);
    }

    public SocketSignalProvider(SignalConnection conn, ImportantTaskExecutor importantTaskExecutor, Scheduler scheduler) {
        if (conn == null) {
            this.signalConnection = new NettySignalConnection();
        } else {
            this.signalConnection = conn;
        }

        if (importantTaskExecutor == null) {
            importantTaskExecutor = new ImportantTaskExecutor();
            this.link(importantTaskExecutor);
        }

        if (scheduler == null) {
            scheduler = new HashedWheelScheduler("SocketSignalProvider");
            this.link((Destroyable) scheduler);
        }

        this.scheduler = scheduler;

        // TODO: we need to double check that the connection state hasn't changed while waiting.
        scheduler.onScheduleComplete(this.onScheduleComplete);

        this.setImportantTaskExecutor(importantTaskExecutor);
        this.link(signalConnection);

        this.initEvents();
    }

    private void initEvents() {
        this.link(pingReceivedEvent);
        this.link(connectionChangedEvent);
        this.link(newClientIdReceivedEvent);
        this.link(signalReceivedEvent);
        this.link(exceptionEvent);
        this.link(signalVerificationReceivedEvent);
        this.link(newVersionEvent);
        this.link(presenceReceivedEvent);
        this.link(subscriptionCompleteReceivedEvent);
        this.link(signalCommandReceivedEvent);
        this.link(commandReceivedEvent);

        // TODO: do we really have to do this? Isn't the "importantTaskExecutor" responsible for this?
        this.signalConnection.getConnectEvent().addObserver(sendConnectCommandIfConnectedObserver_ON_CONNECTION_THREAD);

        /**
         * Forward disconnect events up to clients
         */
        this.signalConnection.getDisconnectEvent().addObserver(new ActiveConnectionObserverAdapter<ConnectionHandle>(this, executeDisconnectStateObserver_ON_CONNECTION_THREAD));
        this.signalConnection.getCommandReceivedEvent().addObserver(new ActiveConnectionObserverAdapter<Command>(this, onMessageReceived_ON_CHANNEL_THREAD));
        this.signalConnection.getPingEventReceivedEvent().addObserver(new ActiveConnectionObserverAdapter<PingEvent>(this, pingReceivedEvent));

        // the ActiveConnectionObserverAdapter will filter out old noise and adapt over the "sender" to the currentConnection if/only if they are active.
        this.signalConnection.getExceptionEvent().addObserver(new ActiveConnectionObserverAdapter<String>(this, exceptionEvent));
        /**
         * Observe our own version changed events so we can stay in sync internally
         */
        newVersionEvent.addObserver(updateVersionsOnVersionChanged);
        newClientIdReceivedEvent.addObserver(updateStateOnNewClientIdReceived);
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

    private final Observer<ConnectionHandle> sendConnectCommandIfConnectedObserver_ON_CONNECTION_THREAD = new Observer<ConnectionHandle>() {

        /**
         * The NettySignalConnection will call this method when a TCP socket connection is attempted.
         */
        @Override
        public void notify(Object sender, final ConnectionHandle socketConnectionHandle) {
            // We are in the SignalConnection executor thread.
            // it's dicey to sync on the SignalProvider object (deadlock?)
            synchronized (signalConnection) {
                if (connectingFuture != null) {
                    LOGGER.warn(String.format("We were currently 'connecting' and got a %s hit", this));
                    return;
                } else if (socketConnectionHandle == null) {
                    throw new NullPointerException("The socketConnectionHandle can never be null!");
                }

                SignalProviderConnectionHandle connectionHandle;

                if (getCurrentConnection() == null) {
                    ensureState(ConnectionState.DISCONNECTED);

                    // we just transitioned to a connected state!
                    connectionHandle = createAndSetActiveSelfHealingSignalProviderConnection();
                    connectionHandle.setConnectionHandle(socketConnectionHandle);
                    currentSignalProviderConnection = connectionHandle;
                }

                final SignalProviderConnectionHandle finalSignalProviderConnectionHandle = (SignalProviderConnectionHandle) getCurrentConnection();

                Asserts.assertTrue(finalSignalProviderConnectionHandle != null, "getCurrentConnection() was null!");
                Asserts.assertTrue(finalSignalProviderConnectionHandle.isFor(socketConnectionHandle), String.format("getCurrentConnection() said this was for a different connection (%s/%s)!", socketConnectionHandle, finalSignalProviderConnectionHandle.connectionHandle));

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

                    connectCommandFuture.addObserver(new HandleConnectCommandIfDoneObserver(finalSignalProviderConnectionHandle));

                    connectCommandFuture.addObserver(new Observer<ObservableFuture<ConnectCommand>>() {
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
                    });
                }
            }
        }

        @Override
        public String toString() {
            return "sendConnectCommandIfConnectedObserver";
        }
    };

    private final Observer<ConnectionHandle> executeDisconnectStateObserver_ON_CONNECTION_THREAD = new Observer<ConnectionHandle>() {

        @Override
        public void notify(Object sender, ConnectionHandle socketConnectionHandle) {
            ConnectionHandle myConnectionHandle = (ConnectionHandle) sender;

            executeDisconnect(myConnectionHandle);
        }

        @Override
        public String toString() {
            return "executeDisconnectStateObserver_ON_CONNECTION_THREAD";
        }
    };

    private void executeDisconnect(ConnectionHandle connectionHandle) {
        synchronized (signalConnection) {
            final SignalProviderConnectionHandle finalSignalProviderConnectionHandle = currentSignalProviderConnection;

            if (finalSignalProviderConnectionHandle == null) {
                LOGGER.error("Does not seem to be an active ConnectionHandle. Quitting executeDisconnect()");
                return;
            } else if (finalSignalProviderConnectionHandle != connectionHandle) {
                LOGGER.error("The connectionHandles didnt agree, so not executing the disconnect events");
                return;
            }

            // We have the SignalConnection lock so we're allowed to set this.
            currentSignalProviderConnection = null;
            ((SignalProviderConnectionHandle) connectionHandle).destroy();

            // TODO: is this the right connection we should use?
            connectionChangedEvent.notifyObservers(connectionHandle, Boolean.FALSE);

            connectionHandle.getDisconnectFuture().setSuccess(connectionHandle);
        }
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
        Asserts.assertTrue(connectionHandle == currentSignalProviderConnection, "Connections not matched up?");
//        currentSignalProviderConnection = new SignalProviderConnection(0, this, connection);

        // If the state has changed then notify
        connectionChangedEvent.notifyObservers(connectionHandle, connected);
    }

    public ConnectionState getConnectionState() {
        synchronized (signalConnection) {

            ConnectionState signalConnectionState = signalConnection.getConnectionState();
            final SignalProviderConnectionHandle connectionHandle = (SignalProviderConnectionHandle) getCurrentConnection();

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
    public String getClientId() {
        return clientId;
    }

    @Override
    public Presence getPresence() {
        return presence;
    }

    @Override
    public void setPresence(Presence presence) {
        this.presence = presence;
    }

    @Override
    public Map<String, Long> getVersions() {
        return versions;
    }

    @Override
    public void setVersions(Map<String, Long> versions) {
        this.versions = versions;
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect() {
        return connect(originalClientId, null, null);
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect(String clientId) {
        return connect(clientId, null, null);
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect(String clientId, Map<String, Long> versions) {
        return connect(clientId, versions, presence);
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> connect(String clientId, final Map<String, Long> versions, Presence presence) {

        // if already connected, return a nonfailing future.
        if (isConnected()) {
            return new FakeFailingObservableFuture<ConnectionHandle>(this, new IllegalStateException("Already connected"));
        } else if (connectingFuture != null) {
            LOGGER.debug(String.format("Returning %s since it's still active", connectingFuture));
            return connectingFuture;
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

        synchronized (signalConnection) {

            Asserts.assertTrue(this.currentSignalProviderConnection == null, "Should be no other connection");

            final SignalProviderConnectionHandle finalSignalProviderConnectionHandle = createAndSetActiveSelfHealingSignalProviderConnection();

            final NestedObservableFuture<ConnectionHandle> finalConnectingFuture = createSelfHealingConnectingFuture(finalSignalProviderConnectionHandle);
            this.connectingFuture = finalConnectingFuture;

            try {
                // This future already has an underlying timeout.
                // this signalConnection.connect() is
                ObservableFuture<ConnectionHandle> requestFuture = signalConnection.connect();

                requestFuture.addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
                    @Override
                    public void notify(Object sender, ObservableFuture<ConnectionHandle> signalConnectionFuture) {
                        synchronized (signalConnection) {
                            synchronized (finalConnectingFuture) {
                                if (finalConnectingFuture.isCancelled()) {
                                    tearDownConnection(finalSignalProviderConnectionHandle);
                                    // oh shit the returned future was cancelled!
                                    Asserts.assertTrue(connectingFuture != finalConnectingFuture, "The futures shouldn't be the same!");
                                    return;
                                }

                                final ConnectionHandle c = getCurrentConnection();
                                if (signalConnectionFuture.isCancelled()) {
                                    tearDownConnection(finalSignalProviderConnectionHandle);
                                    LOGGER.warn("Our connection was cancelled. " + signalConnectionFuture);
                                    finalConnectingFuture.setFailure(new Exception(String.format("The connectionHandles didn't match up. Was %s expected %s", c, finalSignalProviderConnectionHandle)));
                                    return;
                                } else if (signalConnectionFuture.isFailed()) {
                                    tearDownConnection(finalSignalProviderConnectionHandle);
                                    LOGGER.warn("Our connection was failed. " + signalConnectionFuture);
                                    finalConnectingFuture.setFailure(signalConnectionFuture.getCause());
                                    return;
                                } else if (finalSignalProviderConnectionHandle != c) {
                                    tearDownConnection(finalSignalProviderConnectionHandle);
                                    LOGGER.error(String.format("The connectionHandles didn't match up. Was %s expected %s", c, finalSignalProviderConnectionHandle));
                                    finalConnectingFuture.setFailure(new Exception(String.format("The connectionHandles didn't match up. Was %s expected %s", c, finalSignalProviderConnectionHandle)));
                                    return;
                                }

                                if (!signalConnectionFuture.isSuccess()) {
                                    try {
                                        ensureState(ConnectionState.DISCONNECTED);

                                        finalSignalProviderConnectionHandle.destroy();
                                        LOGGER.error(String.format("2 Set currentSignalProviderConnection to null! %s", Thread.currentThread()));
                                        currentSignalProviderConnection = null;
                                        connectingFuture = null;
                                    } finally {
                                        finalConnectingFuture.setFailure(new Exception("Couldn't connect!"));
                                    }
                                    return;
                                }

                                try {
                                    ensureState(ConnectionState.CONNECTED);
                                } catch (Exception e) {
                                    finalConnectingFuture.setFailure(e);
                                    throw new RuntimeException(e);
                                }

                                synchronized (finalSignalProviderConnectionHandle) {
                                    final ConnectionHandle connectionHandle = signalConnectionFuture.getResult();

                                    // set the currently active connection's internal connection.
                                    finalSignalProviderConnectionHandle.setConnectionHandle(connectionHandle);

                                    sendConnectCommand(finalConnectingFuture, finalSignalProviderConnectionHandle, finalClientId, versions);
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {
                connectingFuture = null;
                throw new RuntimeException(e);
            }


            return finalConnectingFuture;
        }
    }

    private void tearDownConnection(SignalProviderConnectionHandle connectionHandle) {
        connectionHandle.destroy();
        LOGGER.error(String.format("2 Set currentSignalProviderConnection to null! %s", Thread.currentThread()));
        currentSignalProviderConnection = null;
        connectingFuture = null;
    }

    private void sendConnectCommand(final ObservableFuture<ConnectionHandle> finalConnectingFuture, final SignalProviderConnectionHandle connectionHandle, final String clientId, Map<String, Long> versions) {
        // send in the connect command (will queue up and execute in our signalProvider.executor
        // so we must be sure not to block (it's the current thread we're on right now!)).
        ObservableFuture<ConnectCommand> sendConnectCommandFuture
                = writeConnectCommandAsyncWithTimeoutBakedIn(connectionHandle, clientId, versions);

        sendConnectCommandFuture.addObserver(new HandleConnectCommandIfDoneObserver(connectionHandle));

        sendConnectCommandFuture.addObserver(new CascadeSuccessToFuture(connectionHandle, finalConnectingFuture));

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
//                if (finalConnectingFuture.isCancelled()) {
//                    LOGGER.warn("Our connectingFuture was cancelled!");
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
//                            LOGGER.debug("Success finalConnectingFuture!");
//                            connectionHandle.finishedActionConnect = true;
//                        } finally {
//
//                        }
//                    } else {
//                        // NOTE: what thread are we in? is this a deadlock?
//                        // who's job is it to tear down the connection?
//                        NestedObservableFuture.syncState(future, finalConnectingFuture, connectionHandle);
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

    private class CascadeSuccessToFuture implements Observer<ObservableFuture<ConnectCommand>> {

        final ConnectionHandle connectionHandle;
        final ObservableFuture<ConnectionHandle> finalConnectingFuture;

        private CascadeSuccessToFuture(ConnectionHandle connectionHandle, ObservableFuture<ConnectionHandle> finalConnectingFuture) {
            this.connectionHandle = connectionHandle;
            this.finalConnectingFuture = finalConnectingFuture;
        }

        @Override
        public void notify(Object sender, ObservableFuture<ConnectCommand> future) {
            synchronized (signalConnection) {
                if (future.isSuccess()) {
                    finalConnectingFuture.setSuccess(connectionHandle);
                } else {
                    NestedObservableFuture.syncState(future, finalConnectingFuture, connectionHandle);
                }
            }
        }
    }

    private class HandleConnectCommandIfDoneObserver implements Observer<ObservableFuture<ConnectCommand>> {

        private final SignalProviderConnectionHandle connectionHandle;

        private HandleConnectCommandIfDoneObserver(SignalProviderConnectionHandle connectionHandle) {
            this.connectionHandle = connectionHandle;
        }

        @Override
        public void notify(Object sender, ObservableFuture<ConnectCommand> future) {
            if (!future.isSuccess()) {
                // shit!
                return;
            } else {

            }

            ConnectCommand connectCommand = future.getResult();

            connectionHandle.finishedActionConnect = true;

            handleConnectCommand(connectionHandle, connectCommand);
        }
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect() {
        return disconnect(false);
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect(boolean causedByNetwork) {
        synchronized (signalConnection) {
            ConnectionState state = getConnectionState();
            switch (state) {
                case CONNECTING:
                    // we'll be interrupting the signalConnection, but that's ok.
                case CONNECTED:
                    // we're interrupting our tasks
                    connectingFuture.cancel();
                    break;
                case AUTHENTICATED:
                    // we are allowed to disconnect in this case.
                    break;
                case DISCONNECTING:
                    return getCurrentConnection().getDisconnectFuture();
                case DISCONNECTED:
                    return new FakeFailingObservableFuture<ConnectionHandle>(this, new IllegalStateException("Not currently connected"));
            }

            final SignalProviderConnectionHandle finalConnectionHandle = currentSignalProviderConnection;

            Asserts.assertTrue(finalConnectionHandle != null, "The current connectionHandle must never disagree with the stateManager!");
            if (finalConnectionHandle == null) return null; // not reachable. just here for compiler warnings.

            synchronized (finalConnectionHandle) {
                ObservableFuture<ConnectionHandle> disconnectFuture = signalConnection.disconnect(causedByNetwork);

                Asserts.assertTrue(disconnectFuture != null, "DisconnectFuture null?");
                if (finalConnectionHandle.connectionHandle != null) {
                    // it might be null if the connection was never fully baked.
                    Asserts.assertTrue(disconnectFuture == finalConnectionHandle.connectionHandle.getDisconnectFuture(), "The different handles didnt agree?");
                }

                return finalConnectionHandle.getDisconnectFuture();
            }
        }
    }

    private SignalProviderConnectionHandle createAndSetActiveSelfHealingSignalProviderConnection() {
        SignalProviderConnectionHandle connection = new SignalProviderConnectionHandle(ID.incrementAndGet(), this);
        this.currentSignalProviderConnection = connection;

        connection.getDisconnectFuture().addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                ConnectionHandle connectionHandle = (ConnectionHandle) sender;

                synchronized (SocketSignalProvider.this) {
                    final ConnectionHandle finalConnectionHandle = SocketSignalProvider.this.currentSignalProviderConnection;

                    if (finalConnectionHandle == connectionHandle) {
                        executeDisconnect(finalConnectionHandle);
                    }
                }
            }
        });

        return connection;
    }

    private NestedObservableFuture<ConnectionHandle> createSelfHealingConnectingFuture(ConnectionHandle connectionHandle) {
        NestedObservableFuture<ConnectionHandle> future = new NestedObservableFuture<ConnectionHandle>(connectionHandle);

        future.addObserver(resetConnectingFutureIfMatchingObserver);
        future.addObserver(notifyConnectedOnFinishObserver);

        return future;
    }

    private final Observer<ObservableFuture<ConnectionHandle>> resetConnectingFutureIfMatchingObserver = new Observer<ObservableFuture<ConnectionHandle>>() {
        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> future) {
            synchronized (signalConnection) {
                if (future == connectingFuture) {
                    connectingFuture = null;
                }
            }
        }
    };

    private final Observer<ObservableFuture<ConnectionHandle>> notifyConnectedOnFinishObserver = new Observer<ObservableFuture<ConnectionHandle>>() {
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
            final ConnectionHandle connectionHandle = getCurrentConnection();
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
        newClientIdReceivedEvent.notifyObservers(getCurrentConnection(), c);

        LOGGER.error("resetAndDisconnect called: " + connectingFuture);

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

    private boolean isConnecting() {
        return connectingFuture != null && !connectingFuture.isDone();
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
                // TODO: what are the initial values?
                String host = null;
                int port = 0;

                if (!StringUtil.EMPTY_STRING.equals(command.getHost())) {
                    host = command.getHost();
                }

                if (command.getPort() > 0) {
                    port = command.getPort();
                }

                this.signalConnection.setAddress(new InetSocketAddress(host, port));

                LOGGER.debug(String.format("We are going to connect again %d seconds from now", command.getReconnectDelay()));

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
            handleCommands((SignalProviderConnectionHandle) getCurrentConnection(), commands);
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

    private class TransitionThreadObserver<T> implements Observer<T> {

        private final Observer<T> observer;
        private final Executor executor;

        public TransitionThreadObserver(Executor executor, Observer<T> observer) {
            this.observer = observer;
            this.executor = executor;
        }

        @Override
        public void notify(final Object sender, final T item) {
            Executor e = executor == null ? SimpleExecutor.getInstance() : executor;

            e.execute(new Runnable() {
                @Override
                public void run() {
                    observer.notify(sender, item);
                }
            });
        }

        @Override
        public String toString() {
            return observer.toString();
        }
    }

    public ConnectionHandle getCurrentConnection() {
        return currentSignalProviderConnection;
    }

    @Override
    protected void onDestroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private void ensureState(ConnectionState state) {
        final ConnectionState existingState = getConnectionState();

        Asserts.assertTrue(existingState == state, String.format("The state was supposed to be %s but was %s", state, existingState));
    }

    private static class ActiveConnectionObserverAdapter<T> implements Observer<T> {

        private final Observer<T> observer;
        private final SocketSignalProvider signalProvider;

        private ActiveConnectionObserverAdapter(SocketSignalProvider signalProvider, Observer<T> observer) {
            this.observer = observer;
            this.signalProvider = signalProvider;
        }

        @Override
        public void notify(Object sender, T data) {
            ConnectionHandle connectionHandle = (ConnectionHandle) sender;

            final SignalProviderConnectionHandle signalProviderConnectionHandle = (SignalProviderConnectionHandle) signalProvider.getCurrentConnection();
            if (signalProviderConnectionHandle == null) {
                LOGGER.error("The signalProviderConnection is null, so it must be inactive. Quitting.");
                return;
            }

            synchronized (signalProviderConnectionHandle) {
                if (signalProviderConnectionHandle.isDestroyed()) {
                    LOGGER.error("The signalProviderConnection is not active. Quitting.");
                    return;
                } else if (!signalProviderConnectionHandle.isFor(connectionHandle)) {
                    LOGGER.error("The signalProviderConnection is not for the current connection. Quitting.");
                    return;
                }

                observer.notify(signalProviderConnectionHandle, data);
            }
        }
    }
}

