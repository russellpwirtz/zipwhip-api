package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.NestedObservableFuture;
import com.zipwhip.api.signals.*;
import com.zipwhip.api.signals.commands.*;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.concurrent.FutureUtil;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.DebuggingExecutor;
import com.zipwhip.executors.FakeObservableFuture;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.important.Scheduler;
import com.zipwhip.important.schedulers.HashedWheelScheduler;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.FutureDateUtil;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

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

    private final ObservableHelper<PingEvent> pingReceivedEvent;
    private final ObservableHelper<List<Signal>> signalReceivedEvent;
    private final ObservableHelper<List<SignalCommand>> signalCommandReceivedEvent;
    private final ObservableHelper<Void> signalVerificationReceivedEvent;
    private final ObservableHelper<Command> commandReceivedEvent;
    private final ObservableHelper<Boolean> presenceReceivedEvent;
    private final ObservableHelper<String> newClientIdReceivedEvent;

    private final ObservableHelper<Boolean> connectionChangedEvent;
    private final ObservableHelper<String> exceptionEvent;
    private final ObservableHelper<VersionMapEntry> newVersionEvent;
    private final ObservableHelper<SubscriptionCompleteCommand> subscriptionCompleteReceivedEvent;

    protected final SignalConnection connection;
    protected final Scheduler scheduler;
    protected final Executor executor;

    private final StateManager<SignalProviderState> stateManager;

    private ImportantTaskExecutor importantTaskExecutor;

    private String clientId;
    private String originalClientId; //So we can detect change

    private Presence presence;
    private Map<String, Long> versions = new HashMap<String, Long>();
    private final Map<String, SlidingWindow<Command>> slidingWindows = new HashMap<String, SlidingWindow<Command>>();
    private ObservableFuture<Boolean> connectingFuture;

    public SocketSignalProvider() {
        this(new NettySignalConnection());
    }

    public SocketSignalProvider(SignalConnection conn) {
        this(conn, null, null, null);
    }

    public SocketSignalProvider(SignalConnection conn, Executor executor, ImportantTaskExecutor importantTaskExecutor, Scheduler scheduler) {
        if (conn == null) {
            this.connection = new NettySignalConnection();
        } else {
            this.connection = conn;
        }

        if (executor == null) {
//            this.executor = SimpleExecutor.getInstance();
            this.executor = new DebuggingExecutor(Executors.newSingleThreadExecutor(new NamedThreadFactory("SocketSignalProvider-"))) {
                @Override
                public synchronized void execute(Runnable command) {
                    super.execute(command);    //To change body of overridden methods use File | Settings | File Templates.
                }

                @Override
                public String toString() {
                    return "[executor:SocketSignalProvider running:" + currentItem + " queue:" + runnableSet + "]";
                }
            };

            // we created it, so we destroy it.
            this.link(new DestroyableBase() {
                @Override
                public void onDestroy() {
                    ((ExecutorService)((DebuggingExecutor) SocketSignalProvider.this.executor).executor).shutdownNow();
                }
            });
        } else {
            this.executor = executor;
        }


        connectionChangedEvent = new ObservableHelper<Boolean>(executor) {
            @Override
            public String toString() {
                return "connectionChangedEvent/" + super.toString();
            }
        };
        pingReceivedEvent = new ObservableHelper<PingEvent>(executor) {
            @Override
            public String toString() {
                return "pingReceivedEvent/" + super.toString();
            }
        };
        newClientIdReceivedEvent = new ObservableHelper<String>(executor) {
            @Override
            public String toString() {
                return "newClientIdReceivedEvent/" + super.toString();
            }
        };
        signalReceivedEvent = new ObservableHelper<List<Signal>>(executor) {
            @Override
            public String toString() {
                return "signalReceivedEvent/" + super.toString();
            }
        };
        signalCommandReceivedEvent = new ObservableHelper<List<SignalCommand>>(executor) {
            @Override
            public String toString() {
                return "signalCommandReceivedEvent/" + super.toString();
            }
        };
        exceptionEvent = new ObservableHelper<String>(executor) {
            @Override
            public String toString() {
                return "exceptionEvent/" + super.toString();
            }
        };
        signalVerificationReceivedEvent = new ObservableHelper<Void>(executor) {
            @Override
            public String toString() {
                return "signalVerificationReceivedEvent/" + super.toString();
            }
        };
        newVersionEvent = new ObservableHelper<VersionMapEntry>(executor) {
            @Override
            public String toString() {
                return "newVersionEvent/" + super.toString();
            }
        };
        presenceReceivedEvent = new ObservableHelper<Boolean>(executor) {
            @Override
            public String toString() {
                return "presenceReceivedEvent/" + super.toString();
            }
        };
        subscriptionCompleteReceivedEvent = new ObservableHelper<SubscriptionCompleteCommand>(executor) {
            @Override
            public String toString() {
                return "subscriptionCompleteReceivedEvent/" + super.toString();
            }
        };
        commandReceivedEvent = new ObservableHelper<Command>(executor) {
            @Override
            public String toString() {
                return "commandReceivedEvent/" + super.toString();
            }
        };

        if (importantTaskExecutor == null) {
            importantTaskExecutor = new ImportantTaskExecutor();
            this.link(importantTaskExecutor);
        }

        if (scheduler == null) {
            scheduler = new HashedWheelScheduler("SocketSignalProvider");
            this.link((Destroyable) scheduler);
        }

        this.scheduler = scheduler;
        scheduler.onScheduleComplete(new TransitionThreadObserver<String>(executor, this.onScheduleComplete));

        this.setImportantTaskExecutor(importantTaskExecutor);
        this.link(connection);

        StateManager<SignalProviderState> m;
        try {
            m = SignalProviderStateManagerFactory.getInstance().create();
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

        this.connection.onMessageReceived(new TransitionThreadObserver<Command>(null, onMessageReceived));
        this.connection.onConnect(new TransitionThreadObserver<Boolean>(executor, sendConnectCommandIfConnected));

        /**
         * Forward disconnect events up to clients
         */
        this.connection.onDisconnect(new TransitionThreadObserver<Boolean>(executor, notifyObserversIfConnectionChangedOnDisconnectObserver));
        this.connection.onPingEvent(pingReceivedEvent);
        this.connection.onExceptionCaught(exceptionEvent);

        /**
         * Observe our own version changed events so we can stay in sync internally
         */
        newVersionEvent.addObserver(updateVersionsOnVersionChanged);
        newClientIdReceivedEvent.addObserver(updateStateOnNewClientIdReceived);
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

//            LOGGER.debug("on message received: /signals/command/received/SIGNAL" + command);

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
                        handleCommands(commandResults);
                        break;
                    case HOLE_FILLED:
                        LOGGER.debug("HOLE_FILLED");
                        handleCommands(commandResults);
                        break;
                    case DUPLICATE_SEQUENCE:
                        LOGGER.warn("DUPLICATE_SEQUENCE");
                        break;
                    case POSITIVE_HOLE:
                        LOGGER.warn("POSITIVE_HOLE");
                        break;
                    case NEGATIVE_HOLE:
                        LOGGER.debug("NEGATIVE_HOLE");
                        handleCommands(commandResults);
                        break;
                    default:
                        LOGGER.warn("UNKNOWN_RESULT");
                }
            } else {
                // Non versioned command, not windowed
                handleCommand(command);
            }
        }

        @Override
        public String toString() {
            return "onMessageReceived";
        }
    };

    private final Observer<Boolean> sendConnectCommandIfConnected = new Observer<Boolean>() {
        /*
        * The NettySignalConnection will call this method when a TCP socket connection is attempted.
        */
        @Override
        public void notify(Object sender, Boolean connected) {
            synchronized (SocketSignalProvider.this) {
                synchronized (connection) {
                    /*
                    * If we have a successful TCP connection then
                    * check if we need to send the connect command.
                    */
                    if (connected) {
                        if (isConnecting()) {
                            return;
                        }

                        final long connectionId = connection.getConnectionId();

                        stateManager.transitionOrThrow(SignalProviderState.CONNECTED);

                        ObservableFuture<ConnectCommand> future = writeConnectCommand();

                        future.addObserver(new Observer<ObservableFuture<ConnectCommand>>() {
                            @Override
                            public void notify(Object sender, final ObservableFuture<ConnectCommand> item) {
                                // do a runIfActive because we are in a different thread.
                                runIfActive(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (connectionId != connection.getConnectionId()) {
                                            LOGGER.warn("The connectionId's changed! Not going to change state.");
                                        } else if (item.isSuccess()) {
                                            stateManager.transitionOrThrow(SignalProviderState.AUTHENTICATED);
                                            // success!
                                            notifyConnected(true);
                                        }
                                    }
                                });
                            }
                        });
                    } else {
                        // not connected. That's already handled via a different observer.
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "sendConnectCommandIfConnected";
        }
    };

    private final Observer<Boolean> notifyObserversIfConnectionChangedOnDisconnectObserver = new Observer<Boolean>() {
        @Override
        public void notify(Object sender, Boolean causedByNetwork) {
            if (stateManager.get() == SignalProviderState.DISCONNECTED) {
                // already disconnected, just quit
                return;
            }
            synchronized (SocketSignalProvider.this) {
                // Ensure that the latch is in a good state for reconnect
                stateManager.transitionOrThrow(SignalProviderState.DISCONNECTED);

                connectionChangedEvent.notifyObservers(sender, Boolean.FALSE);
            }
        }

        @Override
        public String toString() {
            return "notifyObserversIfConnectionChangedOnDisconnectObserver";
        }
    };

    private final Observer<String> updateStateOnNewClientIdReceived = new Observer<String>() {
        @Override
        public void notify(Object sender, String newClientId) {
            clientId = newClientId;
            originalClientId = newClientId;

            if (presence != null)
                presence.setAddress(new ClientAddress(newClientId));
        }

        @Override
        public String toString() {
            return "updateStateOnNewClientIdReceived";
        }
    };

    private void handleCommands(List<Command> commands) {
        for (Command command : commands) {
            handleCommand(command);
        }
    }

    private final Observer<VersionMapEntry> updateVersionsOnVersionChanged = new Observer<VersionMapEntry>() {
        @Override
        public void notify(Object sender, VersionMapEntry version) {
            versions.put(version.getKey(), version.getValue());
        }

        @Override
        public String toString() {
            return "onVersionChanged";
        }
    };

    private void handleCommand(Command command) {

        commandReceivedEvent.notifyObservers(this, command);

        if (command.getVersion() != null && command.getVersion().getValue() > 0) {
            newVersionEvent.notifyObservers(this, command.getVersion());
        }

        if (command instanceof ConnectCommand) {

            handleConnectCommand((ConnectCommand) command);

        } else if (command instanceof DisconnectCommand) {

            handleDisconnectCommand((DisconnectCommand) command);

        } else if (command instanceof SubscriptionCompleteCommand) {

            handleSubscriptionCompleteCommand((SubscriptionCompleteCommand) command);

        } else if (command instanceof SignalCommand) {

            handleSignalCommand((SignalCommand) command);

        } else if (command instanceof PresenceCommand) {

            handlePresenceCommand((PresenceCommand) command);

        } else if (command instanceof SignalVerificationCommand) {

            handleSignalVerificationCommand((SignalVerificationCommand) command);

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
    private ObservableFuture<ConnectCommand> writeConnectCommand() {
        return writeConnectCommandAsyncWithTimeoutBakedIn(clientId, versions);
    }

    /**
     * This future will self cancel if the timeout elapses.
     */
    private ObservableFuture<ConnectCommand> writeConnectCommandAsyncWithTimeoutBakedIn(String clientId, Map<String, Long> versions) {
        return importantTaskExecutor.enqueue(null,
                new ConnectCommandTask(connection, clientId, versions, presence),
                FutureDateUtil.inFuture(connection.getConnectTimeoutSeconds(), TimeUnit.SECONDS));
    }

    private void notifyConnected(boolean connected) {
        // If the state has changed then notify
        connectionChangedEvent.notifyObservers(this, connected);
    }

    public boolean isAuthenticated() {
        if (stateManager.get() == SignalProviderState.AUTHENTICATED) {
//            Asserts.assertTrue(connection.isConnected(), "The connection and stateManager disagreed! (AUTHENTICATED !connected)");

            return true;
        }

        return false;
    }

    public boolean isConnected() {
        if (stateManager.get() == SignalProviderState.CONNECTED) {
//            Asserts.assertTrue(connection.isConnected(), "The connection and stateManager disagreed! (CONNECTED !connected)");

            return true;
        } else if (stateManager.get() == SignalProviderState.AUTHENTICATED) {
//            Asserts.assertTrue(connection.isConnected(), "The connection and stateManager disagreed! (AUTHENTICATED !connected)");

            return true;
        }

        return false;
    }

    @Override
    public SignalProviderState getState() {
        return stateManager.get();
    }

    @Override
    public long getStateVersion() {
        return stateManager.getStateId();
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
    public ObservableFuture<Boolean> connect() throws Exception {
        return connect(originalClientId, null, null);
    }

    @Override
    public ObservableFuture<Boolean> connect(String clientId) throws Exception {
        return connect(clientId, null, null);
    }

    @Override
    public ObservableFuture<Boolean> connect(String clientId, Map<String, Long> versions) throws Exception {
        return connect(clientId, versions, presence);
    }

    @Override
    public synchronized ObservableFuture<Boolean> connect(String clientId, Map<String, Long> versions, Presence presence) throws Exception {

        // if already connected, return a nonfailing future.
        if (isConnected()) {
            return new FakeObservableFuture<Boolean>(this, Boolean.TRUE);
        }

        if (connectingFuture != null) {
            return connectingFuture;
        }

        // keep track of the original one, so we can detect change
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

        // Connect our TCP socket
        final Future<Boolean> socketConnectFuture;

        // If the connection (connection.connect()) happens too fast, then we get a race condition where we
        // try to send in a ConnectCommand twice. We need to set this.connectingFuture to non-null (but really a real future)
        // first, and THEN attach the execute() future to it.
        final NestedObservableFuture<Boolean> finalConnectingFuture = createSelfHealingConnectingFuture();

        this.connectingFuture = finalConnectingFuture;

        try {
            stateManager.transitionOrThrow(SignalProviderState.CONNECTING);
            socketConnectFuture = connection.connect();
        } catch (Exception e) {
            this.connectingFuture = null;
            // oh shit, we crashed!
            LOGGER.warn("Fixed the connectLatch deadlock bug. Killed the latch because got exception connecting.");
            throw e;
        }

        executor.execute(new Runnable() {

            @Override
            public String toString() {
                return "SignalProvider.connect()";
            }

            @Override
            public void run() {
                Asserts.assertTrue(connectingFuture == finalConnectingFuture, "Someone changed the connectionFuture underneath us.");

                boolean connected;

                try {
                    // Block until the TCP connection connects or times out
                    connected = socketConnectFuture.get(connection.getConnectTimeoutSeconds(), TimeUnit.SECONDS);
                } catch (Exception ex) {
                    socketConnectFuture.cancel(true);

                    synchronized (SocketSignalProvider.this) {
                        stateManager.transitionOrThrow(SignalProviderState.DISCONNECTED);

                        finalConnectingFuture.setFailure(ex);

                        return;
                    }
                }

                if (connected) {
                    synchronized (SocketSignalProvider.this) {
                        stateManager.transitionOrThrow(SignalProviderState.CONNECTED);

                        final long connectionId = connection.getConnectionId();

                        // send in the connect command (will queue up and execute in our signalProvider.executor
                        // so we must be sure not to block (it's the current thread we're on right now!)).
                        ObservableFuture<ConnectCommand> sendConnectCommandFuture = writeConnectCommandAsyncWithTimeoutBakedIn(originalClientId, SocketSignalProvider.this.versions);

                        /**
                         * Because the sendConnectCommandFuture will self-timeout, we don't have to do a block/timeout
                         * of our own.
                         *
                         * Regardless of success/failure we will be in the "signalProvider" thread.
                         */
                        sendConnectCommandFuture.addObserver(new Observer<ObservableFuture<ConnectCommand>>() {

                            // WE ARE IN THE "SignalProvider.executor" THREAD
                            // If that executor is "simple" then we're in the connection thread or the scheduler thread.
                            @Override
                            public void notify(Object sender, ObservableFuture<ConnectCommand> future) {
                                // the "sender" is the "task"
                                // we are in the "signalProvider.executor" thread.
                                synchronized (SocketSignalProvider.this) {
                                    if (future.isSuccess()) {
                                        // NOTE: what thread are we in? is this a deadlock?
                                        stateManager.transitionOrThrow(SignalProviderState.AUTHENTICATED);

                                        // THE handleConnectCommand METHOD WILL THROW THE APPROPRIATE EVENTS.
                                        handleConnectCommand(future.getResult());

                                        LOGGER.debug("Success finalConnectingFuture!");
                                        finalConnectingFuture.setSuccess(true);
                                    } else {

                                        // TODO: what happens if the ConnectCommand times out? Who's job is it to tear down?

                                        // NOTE: what thread are we in? is this a deadlock?
                                        // who's job is it to tear down the connection?
                                        NestedObservableFuture.syncStateBoolean(future, finalConnectingFuture);
                                    }

//                                    notifyConnected(future.isSuccess());
                                }
                            }

                            @Override
                            public String toString() {
                                return "sendConnectCommandFuture";
                            }
                        });
                    }
                } else {
                    synchronized (SocketSignalProvider.this) {
                        stateManager.transitionOrThrow(SignalProviderState.DISCONNECTED);

                        finalConnectingFuture.setFailure(new Exception("Couldn't connect!"));
                    }
                }

            }
        });

//        Asserts.assertTrue(finalConnectingFuture == this.connectingFuture, "Make sure no one changed the code later");

        return finalConnectingFuture;
    }

    private NestedObservableFuture<Boolean> createSelfHealingConnectingFuture() {
        NestedObservableFuture<Boolean> future = new NestedObservableFuture<Boolean>(this, executor);

        future.addObserver(new Observer<ObservableFuture<Boolean>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Boolean> item) {
                synchronized (SocketSignalProvider.this) {
                    connectingFuture = null;
                }
            }
        });

        future.addObserver(new Observer<ObservableFuture<Boolean>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Boolean> item) {
                if (item.isSuccess()) {
                    notifyConnected(item.getResult());
                } else {
                    notifyConnected(false);
                }
            }
        });

        return future;
    }

    @Override
    public synchronized ObservableFuture<Void> disconnect() throws Exception {
        return disconnect(false);
    }

    @Override
    public ObservableFuture<Void> disconnect(boolean causedByNetwork) throws Exception {
        if (isConnecting()) {
            // this is an unsafe operation, we're already trying to connect!
            // TODO: probably the best action is to tear everything down
            throw new IllegalStateException("We were connecting and you tried to call disconnect..");
        }

        if (!isConnected()) {
            // we are not currently connected and you called disconnect!
            throw new IllegalStateException("We are not connected and you tried to call disconnect..");
        }

        return FutureUtil.execute(executor, this, connection.disconnect(causedByNetwork));
    }

    public ObservableFuture<Void> runIfActive(final Runnable runnable) {
        final String clientId = this.clientId;
        final boolean wasConnected = isConnected();
        final NestedObservableFuture<Void> resultFuture = new NestedObservableFuture<Void>(this, executor) {
            @Override
            public String toString() {
                return "SignalProvider/runIfActive";
            }
        };

        final long connectionId = connection.getConnectionId();

        try {
            ObservableFuture<Void> future = connection.runIfActive(new Callable<Void>() {

                @Override
                public Void call() {
                    // TODO: check if active.
                    // dont let the clientId be changed while we compare.

                    synchronized (SocketSignalProvider.this) {
                        boolean connected = isConnected();
                        long cId = connection.getConnectionId();
                        if (connectionId != cId) {
                            resultFuture.setFailure(new Exception(String.format("the connectedId changed while waiting, (%b/%b)", connectionId, cId)));
                            LOGGER.warn(String.format(String.format("the connectedId changed while waiting, (%b/%b)", connectionId, cId)));
                            return null;
                        } else if (wasConnected != connected) {
                            resultFuture.setFailure(new Exception(String.format("the connected state changed while waiting, (%b/%b)", wasConnected, connected)));
                            LOGGER.warn(String.format("The two connected states disagree (%b/%b), so not going to execute this runnable %s", wasConnected, connected, runnable));
                            return null;
                        } else if (!StringUtil.equals(SocketSignalProvider.this.getClientId(), clientId)) {
                            LOGGER.warn("We avoided a race condition by detecting the clientId changed. Not going to run this runnable " + runnable);
                            resultFuture.setFailure(new Exception("the ClientId changed while waiting"));
                            return null;
                        }

                        try {
                            runnable.run();
                        } catch (RuntimeException e) {
                            resultFuture.setFailure(e);
                            throw e;
                        }
                    }

                    resultFuture.setSuccess(null);

                    return null;
                }

                @Override
                public String toString() {
                    return "signalProvider/runIfActive/" + runnable;
                }
            });

            // in case the inner one isn't run
            resultFuture.setNestedFuture(future);

        } catch (RuntimeException e) {
            resultFuture.setFailure(e);
            throw e;
        }

        return resultFuture;
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

    // TODO: who calls this because it could be a deadlock
    public synchronized void resetAndDisconnect() throws Exception {
        final String c = clientId = originalClientId = StringUtil.EMPTY_STRING;
        versions.clear();

        synchronized (slidingWindows) {
            for (String key : slidingWindows.keySet()) {
                slidingWindows.get(key).reset();
            }
        }

        newClientIdReceivedEvent.notifyObservers(this, c);

        disconnect(true);
    }

    @Override
    public void nudge() {
        connection.keepalive();
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

    @Override
    protected void onDestroy() {
//        eventExecutor.shutdownNow();
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdownNow();
        }
    }

//    private void succeedTheConnectingFuture() {
//        if (this.connectingFuture != null) {
//            ObservableFuture<Boolean> c = connectingFuture;
//            this.connectingFuture = null;
//            c.setSuccess(true);
//        }
//    }

    private void handleConnectCommand(ConnectCommand command) {
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
                newClientIdReceivedEvent.notifyObservers(this, clientId);
            }

            // we are on the SignalProvider thread, since the events all bootstrap the notify
            // our "executor." We can't trust that the connection is still connected.
            if (versions != null) {
                ObservableFuture<Void> future = runIfActive(new Runnable() {
                    @Override
                    public void run() {
                        // Send a BackfillCommand for each version key - in practice
                        // this is a single key/version
                        for (String key : versions.keySet()) {
                            // TODO: This could fail! Need to enqueue these.
                            connection.send(new BackfillCommand(Collections.singletonList(versions.get(key)), key))
                                    .addObserver((Observer) logIfWriteFailedObserver);
                        }
                    }
                });

                // log if this fails.
                future.addObserver((Observer) logIfWriteFailedObserver);
            }
        }
    }

    private boolean isConnecting() {
        return connectingFuture != null && !connectingFuture.isDone();
    }

    private void handleDisconnectCommand(DisconnectCommand command) {

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

            if (!StringUtil.EMPTY_STRING.equals(command.getHost())) {
                connection.setHost(command.getHost());
            }

            if (command.getPort() > 0) {
                connection.setPort(command.getPort());
            }

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

    private void handlePresenceCommand(PresenceCommand command) {
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

                connection.send(new PresenceCommand(Collections.singletonList(presence)));

            } else {
                LOGGER.debug("Our presence object was empty, so we didn't share it");
            }
        }
    }

    private void handleSignalCommand(SignalCommand command) {
        LOGGER.debug("Handling SignalCommand");

        // Distribute the command and the raw signal to give client's flexibility regarding what data they need
        signalCommandReceivedEvent.notifyObservers(this, Collections.singletonList(command));
        signalReceivedEvent.notifyObservers(this, Collections.singletonList(command.getSignal()));
    }

    private void handleSubscriptionCompleteCommand(SubscriptionCompleteCommand command) {
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
            connection.send(new PresenceCommand(Collections.singletonList(presence)));
            return true;

        } else {
            return false;
        }
    }

    private void handleSignalVerificationCommand(SignalVerificationCommand command) {
        LOGGER.debug("Processing SignalVerificationCommand " + command.toString());
        signalVerificationReceivedEvent.notifyObservers(this, null);
    }

    private final Observer<SlidingWindow.HoleRange> signalHoleObserver = new Observer<SlidingWindow.HoleRange>() {
        @Override
        public void notify(Object sender, SlidingWindow.HoleRange hole) {
            LOGGER.debug("Signal hole detected, requesting backfill for  " + hole.toString());
            connection.send(new BackfillCommand(hole.getRange(), hole.key));
        }
    };

    private final Observer<List<Command>> packetReleasedObserver = new Observer<List<Command>>() {
        @Override
        public void notify(Object sender, List<Command> commands) {
            LOGGER.warn(commands.size() + " packets released due to timeout, leaving a hole.");
            handleCommands(commands);
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
}
