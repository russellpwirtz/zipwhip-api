package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.NestedObservableFuture;
import com.zipwhip.api.signals.*;
import com.zipwhip.api.signals.commands.*;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.concurrent.DefaultObservableFuture;
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
import java.util.concurrent.*;
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

    private final StateManager<ConnectionState> stateManager;

    private ImportantTaskExecutor importantTaskExecutor;

    private String clientId;
    private String originalClientId; //So we can detect change

    private Presence presence;
    private Map<String, Long> versions = new HashMap<String, Long>();
    private final Map<String, SlidingWindow<Command>> slidingWindows = new HashMap<String, SlidingWindow<Command>>();

    private ObservableFuture<ConnectionHandle> connectingFuture;
    private ObservableFuture<ConnectionHandle> disconnectFuture;
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

        StateManager<ConnectionState> m;
        try {
            m = ConnectionStateManagerFactory.getInstance().create();
        } catch (Exception e) {
            m = null;
        }
        stateManager = m;
        if (m == null) {
            throw new RuntimeException("Failed to setup factory");
        }

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
        this.signalConnection.getConnectEvent().addObserver(sendConnectCommandIfConnectedObserver);

        /**
         * Forward disconnect events up to clients
         */
        this.signalConnection.getDisconnectEvent().addObserver(notifyObserversIfConnectionChangedOnDisconnectObserver);
        // this event will only fire if the current connection 'isActive'
        this.signalConnection.getCommandReceivedEvent().addObserver(new ActiveConnectionObserverAdapter<Command>(this, onMessageReceived));
        // the ActiveConnectionObserverAdapter will filter out old noise and adapt over the "sender" to the currentConnection if/only if they are active.
        this.signalConnection.getExceptionEvent().addObserver(new ActiveConnectionObserverAdapter<String>(this, exceptionEvent));

        /**
         * Observe our own version changed events so we can stay in sync internally
         */
        newVersionEvent.addObserver(updateVersionsOnVersionChanged);
        newClientIdReceivedEvent.addObserver(updateStateOnNewClientIdReceived);
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

            synchronized (signalProvider) {
                SignalProviderConnectionHandle signalProviderConnection = (SignalProviderConnectionHandle) signalProvider.getCurrentConnection();
                if (signalProviderConnection == null) {
                    LOGGER.error("The signalProviderConnection is null, so it must be inactive. Quitting.");
                    return;
                }

                synchronized (signalProviderConnection) {
                    if (signalProviderConnection.isDestroyed()) {
                        LOGGER.error("The signalProviderConnection is not active. Quitting.");
                        return;
                    } else if (!signalProviderConnection.isFor(connectionHandle)) {
                        LOGGER.error("The signalProviderConnection is not for the current connection. Quitting.");
                        return;
                    }

                    observer.notify(signalProviderConnection, data);
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
            return "onMessageReceived";
        }
    };

    private final Observer<ConnectionHandle> sendConnectCommandIfConnectedObserver = new Observer<ConnectionHandle>() {

        /**
         * The NettySignalConnection will call this method when a TCP socket connection is attempted.
         */
        @Override
        public void notify(Object sender, final ConnectionHandle socketConnectionHandle) {
            synchronized (SocketSignalProvider.this) {
                if (isConnecting()) {
                    return;
                }

                // sync so the .isActive() state can't change.
                synchronized (SocketSignalProvider.this.signalConnection) {
                    synchronized (socketConnectionHandle) {
                        boolean connected = !socketConnectionHandle.isDestroyed();

                        /**
                         * If we have a successful TCP connection then check if we need to send the connect command.
                         */
                        if (connected) {
                            stateManager.transitionOrThrow(ConnectionState.CONNECTED);

                            final SignalProviderConnectionHandle wrappedConnection = (SignalProviderConnectionHandle) getCurrentConnection();

                            Asserts.assertTrue(wrappedConnection.isFor(socketConnectionHandle), "The connections must agree!");

                            ObservableFuture<ConnectCommand> connectCommandFuture = writeConnectCommandAsyncWithTimeoutBakedIn(socketConnectionHandle);

                            connectCommandFuture.addObserver(new Observer<ObservableFuture<ConnectCommand>>() {
                                @Override
                                public void notify(Object sender, final ObservableFuture<ConnectCommand> future) {

                                    if (future.isSuccess()) {
                                        if (!socketConnectionHandle.isDestroyed()) {
                                            // great, we're still active and we got a success from the future
                                            // transition our state.
                                            stateManager.transitionOrThrow(ConnectionState.AUTHENTICATED);
                                            notifyConnected(wrappedConnection, true);
                                        } else {
                                            // the socketConnection isn't active. Someone else torn it down or we're late to the game.
                                            // just quit.
                                        }
                                    } else {
                                        // we might be in a timeout scenario?
                                        // who's job is it to kill the connection?
                                        // we'll kill it. This shoud cause a reconnect because we passed in true
                                        // the reconnect strategy should kick in?
                                        socketConnectionHandle.disconnect(true);
                                    }
                                }
                            });
                        } else {
                            // not connected. That's already handled via a different observer. TODO: which one?
                        }
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "sendConnectCommandIfConnectedObserver";
        }
    };

    private final Observer<ConnectionHandle> notifyObserversIfConnectionChangedOnDisconnectObserver = new Observer<ConnectionHandle>() {
        @Override
        public void notify(Object sender, ConnectionHandle connectionHandle) {
            synchronized (SocketSignalProvider.this) {
                final SignalProviderConnectionHandle signalProviderConnection = currentSignalProviderConnection;
                if (connectionHandle != null && signalProviderConnection != null && !signalProviderConnection.isFor(connectionHandle)) {
                    // they aren't the same.
                    return;
                } else if (stateManager.get() == ConnectionState.DISCONNECTED) {
                    // already disconnected, just quit
                    return;
                }

                // within this method we are guaranteed to have a non-changing state.

                executeDisconnect(signalProviderConnection);
            }
        }

        @Override
        public String toString() {
            return "notifyObserversIfConnectionChangedOnDisconnectObserver";
        }
    };

    private synchronized void executeDisconnect(ConnectionHandle connectionHandle) {
        if (currentSignalProviderConnection != connectionHandle) {
            LOGGER.error("The connectionHandles didnt agree, so not executing the disconnect events");
            return;
        }

        // Ensure that the latch is in a good state for reconnect
        stateManager.transitionOrThrow(ConnectionState.DISCONNECTED);

        currentSignalProviderConnection.destroy();
        currentSignalProviderConnection.getDisconnectFuture().setSuccess(currentSignalProviderConnection);

        // TODO: is this the right connection we should use?
        connectionChangedEvent.notifyObservers(connectionHandle, Boolean.FALSE);

        // TODO: who's job is it to update this value???
        currentSignalProviderConnection = null;
    }

    private final Observer<String> updateStateOnNewClientIdReceived = new Observer<String>() {

        @Override
        public void notify(Object sender, String newClientId) {
            SignalProviderConnectionHandle connection = (SignalProviderConnectionHandle)sender;

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

    public boolean isAuthenticated() {
        if (stateManager.get() == ConnectionState.AUTHENTICATED) {
//            Asserts.assertTrue(connection.isConnected(), "The connection and stateManager disagreed! (AUTHENTICATED !connected)");

            return true;
        }

        return false;
    }

    public boolean isConnected() {
        if (stateManager.get() == ConnectionState.CONNECTED) {
//            Asserts.assertTrue(connection.isConnected(), "The connection and stateManager disagreed! (CONNECTED !connected)");

            return true;
        } else if (stateManager.get() == ConnectionState.AUTHENTICATED) {
//            Asserts.assertTrue(connection.isConnected(), "The connection and stateManager disagreed! (AUTHENTICATED !connected)");

            return true;
        }

        return false;
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
    public ObservableFuture<ConnectionHandle> connect() throws Exception {
        return connect(originalClientId, null, null);
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect(String clientId) throws Exception {
        return connect(clientId, null, null);
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect(String clientId, Map<String, Long> versions) throws Exception {
        return connect(clientId, versions, presence);
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> connect(String clientId, Map<String, Long> versions, Presence presence) throws Exception {

        // if already connected, return a nonfailing future.
        if (isConnected()) {
            return new FakeFailingObservableFuture<ConnectionHandle>(this, new IllegalStateException("Already connected"));
        } else if (connectingFuture != null) {
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

        Asserts.assertTrue(this.currentSignalProviderConnection == null, "Should be no other connection");
        stateManager.transitionOrThrow(ConnectionState.CONNECTING);

        final SignalProviderConnectionHandle wrappedConnection = createAndSetActiveSelfHealingSignalProviderConnection();
        final NestedObservableFuture<ConnectionHandle> finalConnectingFuture = createSelfHealingConnectingFuture(wrappedConnection);
        this.connectingFuture = finalConnectingFuture;

        // This future already has an underlying timeout.
        ObservableFuture<ConnectionHandle> requestFuture = signalConnection.connect();

        requestFuture.addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                if (item.isSuccess()) {
                    synchronized (SocketSignalProvider.this) {
                        synchronized (signalConnection) {
                            synchronized (wrappedConnection) {
                                stateManager.transitionOrThrow(ConnectionState.CONNECTED);
                                final ConnectionHandle connectionHandle = item.getResult();

                                // set the currently active connection's internal connection.
                                wrappedConnection.setConnectionHandle(connectionHandle);

                                // send in the connect command (will queue up and execute in our signalProvider.executor
                                // so we must be sure not to block (it's the current thread we're on right now!)).
                                ObservableFuture<ConnectCommand> sendConnectCommandFuture
                                        = writeConnectCommandAsyncWithTimeoutBakedIn(wrappedConnection, finalClientId, SocketSignalProvider.this.versions);


                                /**
                                 * Because the sendConnectCommandFuture will self-timeout, we don't have to do a block/timeout
                                 * of our own.
                                 *
                                 * Regardless of success/failure we will be in the "signalProvider" thread.
                                 */
                                sendConnectCommandFuture.addObserver(new Observer<ObservableFuture<ConnectCommand>>() {

                                    // WE ARE IN THE "SignalProvider.executor" THREAD if that executor is "simple" then
                                    // we're in the connection thread or the scheduler thread.
                                    @Override
                                    public void notify(Object sender, ObservableFuture<ConnectCommand> future) {

                                        // the "sender" is the "task"
                                        // we are in the "signalProvider.executor" thread.
                                        synchronized (SocketSignalProvider.this) {
                                            if (future.isSuccess()) {
                                                final ConnectCommand connectCommand = future.getResult();

                                                // NOTE: what thread are we in? is this a deadlock?
                                                stateManager.transitionOrThrow(ConnectionState.AUTHENTICATED);

                                                // THE handleConnectCommand METHOD WILL THROW THE APPROPRIATE EVENTS.
                                                handleConnectCommand(wrappedConnection, connectCommand);

                                                LOGGER.debug("Success finalConnectingFuture!");
                                                finalConnectingFuture.setSuccess(wrappedConnection);
                                            } else {
                                                // TODO: what happens if the ConnectCommand times out? Who's job is it to tear down?

                                                // NOTE: what thread are we in? is this a deadlock?
                                                // who's job is it to tear down the connection?
                                                NestedObservableFuture.syncState(future, finalConnectingFuture, wrappedConnection);
                                            }
                                        }
                                    }

                                    @Override
                                    public String toString() {
                                        return "sendConnectCommandFuture";
                                    }
                                });
                            }
                        }
                    }
                } else {
                    synchronized (SocketSignalProvider.this) {
                        stateManager.transitionOrThrow(ConnectionState.DISCONNECTED);

                        wrappedConnection.destroy();


                        finalConnectingFuture.setFailure(new Exception("Couldn't connect!"));
                    }
                }

            }
        });

        return finalConnectingFuture;
    }

    private SignalProviderConnectionHandle createAndSetActiveSelfHealingSignalProviderConnection() {
        SignalProviderConnectionHandle connection = new SignalProviderConnectionHandle(ID.incrementAndGet(), this);
        this.currentSignalProviderConnection = connection;

        connection.getDisconnectFuture().addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                ConnectionHandle connectionHandle = (ConnectionHandle)sender;

                synchronized (SocketSignalProvider.this) {
                    if (SocketSignalProvider.this.currentSignalProviderConnection == connectionHandle) {
                        SocketSignalProvider.this.currentSignalProviderConnection = null;
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
            synchronized (SocketSignalProvider.this) {
                if (future == connectingFuture) {
                    connectingFuture = null;
                }
            }
        }
    };

    private final Observer<ObservableFuture<ConnectionHandle>> notifyConnectedOnFinishObserver = new Observer<ObservableFuture<ConnectionHandle>>() {
        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> future) {
            ConnectionHandle connectionHandle = (SignalProviderConnectionHandle)sender;

            if (future.isSuccess()) {
                Asserts.assertTrue(sender == future.getResult(), "The connections should agree.");
                boolean connected = !connectionHandle.isDestroyed();

                notifyConnected(connectionHandle, connected);
            } else {
                notifyConnected(connectionHandle, false);
            }
        }
    };

    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect() {
        return disconnect(false);
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect(boolean causedByNetwork) {
        if (isConnecting()) {
            // this is an unsafe operation, we're already trying to connect!
            // TODO: probably the best action is to tear everything down
            throw new IllegalStateException("We were connecting and you tried to call disconnect..");
        }

        if (!isConnected()) {
            // we are not currently connected and you called disconnect!
            throw new IllegalStateException("We are not connected and you tried to call disconnect..");
        }

        stateManager.transitionOrThrow(ConnectionState.DISCONNECTING);

        final ConnectionHandle finalConnectionHandle = currentSignalProviderConnection;

        final ObservableFuture<ConnectionHandle> resultFuture = finalConnectionHandle.getDisconnectFuture();

//        currentSignalProviderConnection.getConnectionHandle().disconnect(causedByNetwork);

        final ObservableFuture<ConnectionHandle> requestFuture = signalConnection.disconnect(causedByNetwork);

        requestFuture.addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                executeDisconnect(finalConnectionHandle);
            }
        });

        return resultFuture;
    }

    // TODO: who calls this because it could be a deadlock
    public synchronized ObservableFuture<ConnectionHandle> resetDisconnectAndConnect() {
        final String c = clientId = originalClientId = StringUtil.EMPTY_STRING;
        // TODO: I think this is a bug. The local hashmap being cleared doesnt really do anything on disk.
        versions.clear();

        synchronized (slidingWindows) {
            for (String key : slidingWindows.keySet()) {
                slidingWindows.get(key).reset();
            }
        }

        // todo: is this the right event?
        newClientIdReceivedEvent.notifyObservers(this, c);

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

    private void handleConnectCommand(SignalProviderConnectionHandle connection, ConnectCommand command) {
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
                newClientIdReceivedEvent.notifyObservers(connection, clientId);
            }

            // we are on the SignalProvider thread, since the events all bootstrap the notify
            // our "executor." We can't trust that the connection is still connected.
            if (versions != null) {
                // kind of cheating i guess.
                for (String key : versions.keySet()) {
                    connection.write(new BackfillCommand(Collections.singletonList(versions.get(key)), key))
                            .addObserver((Observer) logIfWriteFailedObserver);
                }
            }
        }
    }

    private boolean isConnecting() {
        return connectingFuture != null && !connectingFuture.isDone();
    }

    private void handleDisconnectCommand(SignalProviderConnectionHandle connection, DisconnectCommand command) {

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

                this.signalConnection.send(new PresenceCommand(Collections.singletonList(presence)));

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
            handleCommands(currentSignalProviderConnection, commands);
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

    private static class CopyStateObserver<T> implements Observer<T> {

        final Observer<T> observer;

        private CopyStateObserver(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public void notify(Object sender, T item) {
            observer.notify(sender, item);
        }
    }

    public ConnectionHandle getCurrentConnection() {
        return currentSignalProviderConnection;
    }

    @Override
    protected void onDestroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

