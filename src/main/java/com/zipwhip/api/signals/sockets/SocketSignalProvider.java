package com.zipwhip.api.signals.sockets;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.api.signals.commands.BackfillCommand;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.DisconnectCommand;
import com.zipwhip.api.signals.commands.NoopCommand;
import com.zipwhip.api.signals.commands.PresenceCommand;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.api.signals.commands.SignalVerificationCommand;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.FakeFuture;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;

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

    private final ObservableHelper<PingEvent> pingEvent = new ObservableHelper<PingEvent>();
    private final ObservableHelper<Boolean> connectEvent = new ObservableHelper<Boolean>();
    private final ObservableHelper<String> newClientIdEvent = new ObservableHelper<String>();
    private final ObservableHelper<List<Signal>> signalEvent = new ObservableHelper<List<Signal>>();
    private final ObservableHelper<List<SignalCommand>> signalCommandEvent = new ObservableHelper<List<SignalCommand>>();
    private final ObservableHelper<String> exceptionEvent = new ObservableHelper<String>();
    private final ObservableHelper<Void> signalVerificationEvent = new ObservableHelper<Void>();
    private final ObservableHelper<VersionMapEntry> newVersionEvent = new ObservableHelper<VersionMapEntry>();
    private final ObservableHelper<Boolean> presenceReceivedEvent = new ObservableHelper<Boolean>();
    private final ObservableHelper<SubscriptionCompleteCommand> subscriptionCompleteEvent = new ObservableHelper<SubscriptionCompleteCommand>();
    private final ObservableHelper<Command> commandReceivedEvent = new ObservableHelper<Command>();

    private CountDownLatch connectLatch;
    private SignalConnection connection = new NettySignalConnection();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String clientId;
    private String originalClientId; //So we can detect change

    private boolean connectionStateSwitch; // The previous connection state
    private boolean connectionNegotiated; // Have we finished negotiating with SignalServer

    private Presence presence;
    private Map<String, Long> versions = new HashMap<String, Long>();
    private Map<String, SlidingWindow<Command>> slidingWindows = new HashMap<String, SlidingWindow<Command>>();

    public SocketSignalProvider() {
        this(new NettySignalConnection());
    }

    public SocketSignalProvider(SignalConnection connection) {

        this.connection = connection;
        this.link(this.connection);

        this.link(pingEvent);
        this.link(connectEvent);
        this.link(newClientIdEvent);
        this.link(signalEvent);
        this.link(exceptionEvent);
        this.link(signalVerificationEvent);
        this.link(newVersionEvent);
        this.link(presenceReceivedEvent);
        this.link(subscriptionCompleteEvent);
        this.link(signalCommandEvent);

        connection.onMessageReceived(new Observer<Command>() {
            /**
             * The NettySignalConnection will call this method when there's an
             * event from the remote SignalServer.
             *
             * @param sender The sender might not be the same object every time.
             * @param command Rich object representing the command received from the SignalServer.
             */
            @Override
            public void notify(Object sender, Command command) {

                // Check if this command has a valid version number associated with it...
                if (command.getVersion() != null && command.getVersion().getValue() > 0) {

                    String versionKey = command.getVersion().getKey();

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
        });

        connection.onConnect(new Observer<Boolean>() {
            /*
                * The NettySignalConnection will call this method when a TCP socket connection is attempted.
                */
            @Override
            public void notify(Object sender, Boolean connected) {
                /*
                     * If we have a successful TCP connection then
                     * check if we need to send the connect command.
                     */
                if (connected) {
                    sendConnect();
                }
            }
        });

        /*
              Forward connect events up to clients
           */
        connection.onConnect(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean connected) {
                // If the state has changed then notify
                if (connectionStateSwitch ^ connected) {
                    connectionStateSwitch = connected;
                    connectEvent.notifyObservers(sender, connected);
                }
            }
        });

        /*
              Forward disconnect events up to clients
           */
        connection.onDisconnect(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean disconnected) {

                // Ensure that the latch is in a good state for reconnect
                if (connectLatch != null) {
                    connectLatch.countDown();
                }

                connectionNegotiated = false;

                // If the state has changed then notify
                if (connectionStateSwitch) {
                    connectionStateSwitch = false;
                    connectEvent.notifyObservers(sender, Boolean.FALSE);
                }
            }
        });

        /*
              Forward ping events up to clients
           */
        connection.onPingEvent(new Observer<PingEvent>() {
            @Override
            public void notify(Object sender, PingEvent item) {
                pingEvent.notifyObservers(sender, item);
            }
        });

        /*
              Forward connection exceptions up to clients
           */
        connection.onExceptionCaught(new Observer<String>() {
            @Override
            public void notify(Object sender, String message) {
                exceptionEvent.notifyObservers(sender, message);
            }
        });

        /*
              Observe our own version changed events so we can stay in sync internally
           */
        onVersionChanged(new Observer<VersionMapEntry>() {
            @Override
            public void notify(Object sender, VersionMapEntry version) {
                versions.put(version.getKey(), version.getValue());
            }
        });

        onNewClientIdReceived(new Observer<String>() {
            @Override
            public void notify(Object sender, String newClientId) {
                clientId = newClientId;
                originalClientId = newClientId;

                if (presence != null)
                    presence.setAddress(new ClientAddress(newClientId));
            }
        });
    }

    private void handleCommands(List<Command> commands) {
        for (Command command : commands) {
            handleCommand(command);
        }
    }

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
    private void sendConnect() {
        if ((connectLatch == null) || (connectLatch.getCount() == 0)) {
            connection.send(new ConnectCommand(clientId, versions));
        }
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected() && connectionNegotiated;
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
    public Future<Boolean> connect() throws Exception {
        return connect(originalClientId, null, null);
    }

    @Override
    public Future<Boolean> connect(String clientId) throws Exception {
        return connect(clientId, null, null);
    }

    @Override
    public Future<Boolean> connect(String clientId, Map<String, Long> versions) throws Exception {
        return connect(clientId, versions, presence);
    }

    @Override
    public Future<Boolean> connect(String clientId, Map<String, Long> versions, Presence presence) throws Exception {

        if (isConnected() || ((connectLatch != null) && (connectLatch.getCount() > 0))) {
            LOGGER.debug("Connect requested but already connected or connecting...");
            return new FakeFuture<Boolean>(Boolean.TRUE);
        }

        // This will help us do the connect synchronously
        connectLatch = new CountDownLatch(1);

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
        final Future<Boolean> connectFuture = connection.connect();

        FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() {

                try {
                    // Block until the TCP connection connects or times out
                    connectFuture.get(NettySignalConnection.CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                    if (connection.isConnected()) {

                        connection.send(new ConnectCommand(originalClientId, SocketSignalProvider.this.versions));

                        // block while the signal server is thinking/hanging.
                        boolean countedDown = connectLatch.await(NettySignalConnection.CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                        // If we timed out the latch might still blocking other threads
                        if (!countedDown) {
                            connectLatch.countDown();
                        }

                    } else {
                        // Need to make sure we always count down
                        connectLatch.countDown();
                    }

                } catch (Exception e) {

                    LOGGER.error("Exception in connecting..." + e, e.getCause());

                    // Need to make sure we always count down
                    connectLatch.countDown();
                }

                return isConnected();
            }
        });

        // this background thread stops us from blocking.
        executor.execute(task);
        return task;
    }

    @Override
    public Future<Void> disconnect() throws Exception {

        for (String key : slidingWindows.keySet()) {
            slidingWindows.get(key).destroy();
        }
        slidingWindows.clear();

        return connection.disconnect(false);
    }

    @Override
    public void nudge() {
        connection.keepalive();
    }

    @Override
    public void startPings() {
        connection.startKeepalives();
    }

    @Override
    public void stopPings() {
        connection.stopKeepalives();
    }

    @Override
    public void onSignalReceived(Observer<List<Signal>> observer) {
        signalEvent.addObserver(observer);
    }

    @Override
    public void onSignalCommandReceived(Observer<List<SignalCommand>> observer) {
        signalCommandEvent.addObserver(observer);
    }

    @Override
    public void onConnectionChanged(Observer<Boolean> observer) {
        connectEvent.addObserver(observer);
    }

    @Override
    public void onNewClientIdReceived(Observer<String> observer) {
        newClientIdEvent.addObserver(observer);
    }

    @Override
    public void onSubscriptionComplete(Observer<SubscriptionCompleteCommand> observer) {
        subscriptionCompleteEvent.addObserver(observer);
    }

    @Override
    public void onPhonePresenceReceived(Observer<Boolean> observer) {
        presenceReceivedEvent.addObserver(observer);
    }

    @Override
    public void onSignalVerificationReceived(Observer<Void> observer) {
        signalVerificationEvent.addObserver(observer);
    }

    @Override
    public void onVersionChanged(Observer<VersionMapEntry> observer) {
        newVersionEvent.addObserver(observer);
    }

    @Override
    public void onPingEvent(Observer<PingEvent> observer) {
        pingEvent.addObserver(observer);
    }

    @Override
    public void onExceptionEvent(Observer<String> observer) {
        exceptionEvent.addObserver(observer);
    }

    @Override
    public void onCommandReceived(Observer<Command> observer) {
        commandReceivedEvent.addObserver(observer);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
    }

    private void handleConnectCommand(ConnectCommand command) {

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Handling ConnectCommand " + command.isSuccessful());

        if (command.isSuccessful()) {

            connectionNegotiated = true;

            // copy it over for stale checking
            originalClientId = clientId;

            clientId = command.getClientId();

            if (!StringUtil.equals(clientId, originalClientId)) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Received a new client id: " + clientId);
                }
                // not the same, lets announce
                // announce on a separate thread
                newClientIdEvent.notifyObservers(this, clientId);
            }

            if (versions != null) {
                // Send a BackfillCommand for each version key - in practice
                // this is a single key/version
                for (String key : versions.keySet()) {
                    connection.send(new BackfillCommand(Collections.singletonList(versions.get(key)), key));
                }
            }

            startPings();

        } else {

            connectionNegotiated = false;
        }

        if (connectLatch != null) {
            // we need to countDown the latch, when it hits zero (after this
            // call)
            // the connect ObservableFuture will complete. This gives the caller
            // a way to block on our connection
            connectLatch.countDown();
        }
    }

    private void handleDisconnectCommand(DisconnectCommand command) {

        LOGGER.debug("Handling DisconnectCommand");

        try {
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

                int[] newPortArray = new int[connection.getPorts().length + 1];
                newPortArray[0] = command.getPort();

                for (int i = 0; i < connection.getPorts().length; i++) {
                    newPortArray[i + 1] = connection.getPorts()[i];
                }

                connection.setPorts(newPortArray);
            }

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    try {

                        // Clear the clientId so we will re-up on connect
                        originalClientId = StringUtil.EMPTY_STRING;

                        connect();

                    } catch (Exception e) {
                        LOGGER.error("Error connecting", e);
                    }
                }
            }, command.getReconnectDelay(), TimeUnit.SECONDS);
        }
    }

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
        signalCommandEvent.notifyObservers(this, Collections.singletonList(command));
        signalEvent.notifyObservers(this, Collections.singletonList(command.getSignal()));
    }

    private void handleSubscriptionCompleteCommand(SubscriptionCompleteCommand command) {

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Handling SubscriptionCompleteCommand");

        if (presence != null) {

            if (presence.getAddress() == null) {
                presence.setAddress(new ClientAddress());
            }

            // Set our clientId in case its not already there
            presence.getAddress().setClientId(clientId);

            connection.send(new PresenceCommand(Collections.singletonList(presence)));
        }

        subscriptionCompleteEvent.notifyObservers(this, command);
    }

    private void handleSignalVerificationCommand(SignalVerificationCommand command) {
        LOGGER.debug("Processing SignalVerificationCommand " + command.toString());
        signalVerificationEvent.notifyObservers(this, null);
    }

    private Observer<SlidingWindow.HoleRange> signalHoleObserver = new Observer<SlidingWindow.HoleRange>() {
        @Override
        public void notify(Object sender, SlidingWindow.HoleRange hole) {
            LOGGER.debug("Signal hole detected, requesting backfill for  " + hole.toString());
            connection.send(new BackfillCommand(hole.getRange(), hole.key));
        }
    };

    private Observer<List<Command>> packetReleasedObserver = new Observer<List<Command>>() {
        @Override
        public void notify(Object sender, List<Command> commands) {
            LOGGER.warn(commands.size() + " packets released due to timeout, leaving a hole.");
            handleCommands(commands);
        }
    };

}
