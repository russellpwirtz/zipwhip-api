package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.*;
import com.zipwhip.api.signals.commands.*;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.FakeFuture;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.util.CollectionUtil;
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
public class SocketSignalProvider extends DestroyableBase implements SignalProvider {

    private static final Logger LOGGER = Logger.getLogger(SocketSignalProvider.class);

    private ObservableHelper<PingEvent> pingEvent = new ObservableHelper<PingEvent>();
    private ObservableHelper<Boolean> connectEvent = new ObservableHelper<Boolean>();
    private ObservableHelper<String> newClientIdEvent = new ObservableHelper<String>();
    private ObservableHelper<List<Signal>> signalEvent = new ObservableHelper<List<Signal>>();
    private ObservableHelper<String> exceptionEvent = new ObservableHelper<String>();
    private ObservableHelper<Void> signalVerificationEvent = new ObservableHelper<Void>();
    private ObservableHelper<VersionMapEntry> newVersionEvent = new ObservableHelper<VersionMapEntry>();
    private ObservableHelper<Boolean> presenceReceivedEvent = new ObservableHelper<Boolean>();
    private ObservableHelper<SubscriptionCompleteCommand> subscriptionCompleteEvent = new ObservableHelper<SubscriptionCompleteCommand>();
    private ObservableHelper<Command> commandReceivedEvent = new ObservableHelper<Command>();

    private CountDownLatch connectLatch;
    private SignalConnection connection = new NettySignalConnection();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private String clientId;
    private String originalClientId; //So we can detect change

    private boolean connectionStateSwitch; // The previous connection state
    private boolean connectionNegotiated; // Have we finished negotiating with SignalServer

    private Presence presence;
    private Map<String, Long> versions = new HashMap<String, Long>();

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

                commandReceivedEvent.notifyObservers(this, command);

                // Check if this command has a version number associated with it
                if (command.getVersion() != null && command.getVersion().getValue() >= 0) {
                    newVersionEvent.notifyObservers(this, command.getVersion());
                }

                if (command instanceof ConnectCommand) {

                    handleConnectCommand((ConnectCommand) command);

                } else if (command instanceof DisconnectCommand) {

                    handleDisconnectCommand((DisconnectCommand) command);

                } else if (command instanceof SubscriptionCompleteCommand) {

                    handleSubscriptionCompleteCommand((SubscriptionCompleteCommand) command);

                } else if (command instanceof BacklogCommand) {

                    handleBacklogCommand((BacklogCommand) command);

                } else if (command instanceof SignalCommand) {

                    handleSignalCommand((SignalCommand) command);

                } else if (command instanceof PresenceCommand) {

                    handlePresenceCommand((PresenceCommand) command);

                } else if (command instanceof SignalVerificationCommand) {

                    handleSignalVerificationCommand((SignalVerificationCommand) command);

                } else if (command instanceof NoopCommand) {

                    LOGGER.debug("Received NoopCommand");

                } else {

                    LOGGER.warn("Unrecognised command: " + command.getClass().getSimpleName());
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
                    connectEvent.notifyObservers(sender, false);
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
    }

    /*
     * This method allows us to decouple connection.connect() from provider.connect() for
     * cases when we have been notified by the connection that it has a successful connection.
     */
    private void sendConnect() {
        if (connectLatch == null || connectLatch.getCount() == 0) {
            connection.send(new ConnectCommand(clientId, versions, presence));
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

        if (isConnected() || (connectLatch != null && connectLatch.getCount() > 0)) {
            LOGGER.debug("Connect requested but already connected or connecting...");
            return new FakeFuture<Boolean>(true);
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

        if (!CollectionUtil.isNullOrEmpty(versions)) {
            this.versions = versions;
        }

        // Connect our TCP socket
        final Future<Boolean> connectFuture = connection.connect();

        FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() {

                try {
                    // Block until the TCP connection connections or times out
                    connectFuture.get(NettySignalConnection.CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                    if (connection.isConnected()) {

                        connection.send(new ConnectCommand(originalClientId, SocketSignalProvider.this.versions, SocketSignalProvider.this.presence));

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

                    LOGGER.error(e);

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

        LOGGER.debug("Handling ConnectCommand");

        if (command.isSuccessful()) {

            connectionNegotiated = true;

            // copy it over for stale checking
            originalClientId = clientId;

            clientId = command.getClientId();

            if (!StringUtil.equals(clientId, originalClientId)) {
                // not the same, lets announce
                // announce on a separate thread
                newClientIdEvent.notifyObservers(this, clientId);
            }

        } else {

            connectionNegotiated = false;
        }

        if (connectLatch != null) {
            // we need to countDown the latch, when it hits zero (after this call)
            // the connect NetworkFuture will complete. This gives the caller a way to block on our connection
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

            LOGGER.warn("BANNED by SignalServer!");

            // TODO ban the user somehow?
        }

        // If the command has not said 'ban' or 'stop'
        if (!command.isStop() || !command.isBan()) {

            if (!StringUtil.EMPTY_STRING.equals(command.getHost())) {
                connection.setHost(command.getHost());
            }

            if (command.getPort() > 0) {
                connection.setPort(command.getPort());
            }

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        connect();
                    } catch (Exception e) {
                        LOGGER.error("Error connecting", e);
                    }
                }
            }, command.getReconnectDelay(), TimeUnit.SECONDS);
        }
    }

    private void handleBacklogCommand(BacklogCommand command) {

        LOGGER.debug("Handling BacklogCommand");

        List<Signal> signals = new ArrayList<Signal>();

        for (SignalCommand signalCommand : command.getCommands()) {

            signals.add(signalCommand.getSignal());

            if (signalCommand.getVersion() != null && signalCommand.getVersion().getValue() >= 0) {
                newVersionEvent.notifyObservers(this, signalCommand.getVersion());
            }
        }

        signalEvent.notifyObservers(this, signals);
    }

    private void handlePresenceCommand(PresenceCommand command) {

        LOGGER.debug("Handling PresenceCommand");

        for (Presence presence : command.getPresence()) {
            if (presence.getCategory().equals(PresenceCategory.Phone)) {
                presenceReceivedEvent.notifyObservers(this, presence.getConnected());
            }
        }
    }

    private void handleSignalCommand(SignalCommand command) {
        LOGGER.debug("Handling SignalCommand");
        signalEvent.notifyObservers(this, Collections.singletonList(command.getSignal()));
    }

    private void handleSubscriptionCompleteCommand(SubscriptionCompleteCommand command) {
        LOGGER.debug("Handling SubscriptionCompleteCommand");
        subscriptionCompleteEvent.notifyObservers(this, command);
    }

    private void handleSignalVerificationCommand(SignalVerificationCommand command) {
        LOGGER.debug("Processing SignalVerificationCommand " + command.toString());
        signalVerificationEvent.notifyObservers(this, null);
    }

}
