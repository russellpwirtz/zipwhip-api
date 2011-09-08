package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.*;
import com.zipwhip.api.signals.commands.*;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.FakeFuture;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    private static final Logger logger = Logger.getLogger(SocketSignalProvider.class);

    private ObservableHelper<Boolean> connectEvent = new ObservableHelper<Boolean>();
    private ObservableHelper<String> newClientIdEvent = new ObservableHelper<String>();
    private ObservableHelper<List<Signal>> signalEvent = new ObservableHelper<List<Signal>>();
    private ObservableHelper<Void> signalVerificationEvent = new ObservableHelper<Void>();
    private ObservableHelper<VersionMapEntry> newVersionEvent = new ObservableHelper<VersionMapEntry>();
    private ObservableHelper<List<Presence>> presenceReceivedEvent = new ObservableHelper<List<Presence>>();
    private ObservableHelper<SubscriptionCompleteCommand> subscriptionCompleteEvent = new ObservableHelper<SubscriptionCompleteCommand>();

    private CountDownLatch connectLatch;
    private SignalConnection connection = new NettySignalConnection();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private String clientId;
    private String originalClientId; //so we can detect change

    public SocketSignalProvider() {
        this(new NettySignalConnection());
    }

    public SocketSignalProvider(SignalConnection connection) {

        this.connection = connection;
        this.link(this.connection);

        this.link(connectEvent);
        this.link(newClientIdEvent);
        this.link(signalEvent);
        this.link(signalVerificationEvent);
        this.link(newVersionEvent);
        this.link(presenceReceivedEvent);
        this.link(subscriptionCompleteEvent);

        // TODO I think we want to move this into a setter on the SignalConnection
        // Create A ReconnectStrategy
        ReconnectStrategy strategy = new DefaultReconnectStrategy();
        strategy.setSignalConnection(connection);
        strategy.start();

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

                    logger.debug("Received NoopCommand");

                } else {

                    logger.warn("Unrecognised command: " + command.getClass().getSimpleName());
                }
            }
        });

        connection.onConnect(new Observer<Boolean>() {
            /**
             * The NettySignalConnection will call this method when a TCP socket connection is attempted.
             *
             * @param sender The sender might not be the same object every time.
             * @param connected True, if connected, False if disconnected
             */
            @Override
            public void notify(Object sender, Boolean connected) {

                if (connected) {

                    try {
                        //connect(originalClientId);
                    } catch (Exception e) {

                    }
                }
            }
        });
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected() && StringUtil.exists(clientId);
    }

    @Override
    public String getClientId() {
        return clientId;
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
        return connect(clientId, versions, null);
    }

    @Override
    public Future<Boolean> connect(final String clientId, final Map<String, Long> versions, final Presence presence) throws Exception {

        // TODO move this to the strategy

        if (isConnected()) {
            logger.debug("Connect requested but already connected");
            return new FakeFuture<Boolean>(true);
        }

        // keep track of the original one, so we can detect change
        if (StringUtil.exists(clientId)) {
            originalClientId = clientId;
        }

        // this will help us do the connect synchronously
        connectLatch = new CountDownLatch(1);
        final Future<Boolean> connectFuture = connection.connect();

        FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() {

                try {
                    connectFuture.get(NettySignalConnection.CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

                if (connection.isConnected()) {

                    connection.send(new ConnectCommand(clientId, versions, presence));

                    // block while the signal server is thinking/hanging.
                    try {
                        connectLatch.await(NettySignalConnection.CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                final boolean result = isConnected();

                // queue it up, or else the "connect()" call will still block while the slow-ass observers fire.
                connectEvent.notifyObservers(this, result);

                return result;
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
    public void onPresenceReceived(Observer<List<Presence>> observer) {
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
    protected void onDestroy() {
        executor.shutdownNow();
    }

    private void handleConnectCommand(ConnectCommand command) {

        logger.debug("Handling ConnectCommand");

        if (command.isSuccessful()) {
            // copy it over for stale checking
            originalClientId = clientId;

            clientId = command.getClientId();

            if (!StringUtil.equals(clientId, originalClientId)) {
                // not the same, lets announce
                // announce on a separate thread
                newClientIdEvent.notifyObservers(this, clientId);
            }

            if (connectLatch != null) {
                // we need to countDown the latch, when it hits zero (after this call)
                // the connect Future will complete. This gives the caller a way to block on our connection
                connectLatch.countDown();
            }
        }
    }

    private void handleDisconnectCommand(DisconnectCommand command) {

        logger.debug("Handling DisconnectCommand");

        try {
            disconnect();
        } catch (Exception e) {
            logger.error("Error disconnecting", e);
        }

        if (!command.isStop()) {

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
                        logger.error("Error connecting", e);
                    }
                }
            }, command.getReconnectDelay(), TimeUnit.SECONDS);
        }
    }

    private void handleBacklogCommand(BacklogCommand command) {

        logger.debug("Handling BacklogCommand");

        List<Signal> signals = new ArrayList<Signal>();

        for (SignalCommand signalCommand : command.getCommands()) {

            signals.add(signalCommand.getSignal());

            if (signalCommand.getVersion() != null && signalCommand.getVersion().getValue() >= 0) {
                newVersionEvent.notifyObservers(this, signalCommand.getVersion());
            }
        }

        signalEvent.notifyObservers(this, signals);
    }

    private void handleSignalCommand(SignalCommand command) {
        logger.debug("Handling SignalCommand");
        signalEvent.notifyObservers(this, Collections.singletonList(command.getSignal()));
    }

    private void handleSubscriptionCompleteCommand(SubscriptionCompleteCommand command) {
        logger.debug("Handling SubscriptionCompleteCommand");
        subscriptionCompleteEvent.notifyObservers(this, command);
    }

    private void handlePresenceCommand(PresenceCommand command) {
        logger.debug("Handling PresenceCommand");
        presenceReceivedEvent.notifyObservers(this, command.getPresence());
    }

    private void handleSignalVerificationCommand(SignalVerificationCommand command) {
        logger.debug("Processing SignalVerificationCommand");
        signalVerificationEvent.notifyObservers(this, null);
    }

}
