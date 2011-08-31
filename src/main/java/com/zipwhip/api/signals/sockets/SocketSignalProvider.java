package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.VersionMapEntry;
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

    private SignalConnection connection = new NettySignalConnection();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private String clientId;
    private String originalClientId; //so we can detect change
    private Presence presence;
    private CountDownLatch connectLatch;

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

        connection.onMessageReceived(new Observer<Command>() {
            /**
             * The NettySignalConnection will call this method when there's an
             * event from the remote signal server.
             * 
             * @param sender
             *        The sender might not be the same object every time, so
             *        we'll let it just be object, rather than generics.
             * @param item
             *        Rich object representing the notification.
             */
            @Override
            public void notify(Object sender, Command item) {

                if (item instanceof ConnectCommand) {
                    handleConnectCommand((ConnectCommand) item);
                }
                else if (item instanceof DisconnectCommand) {
                    handleDisconnectCommand((DisconnectCommand) item);
                }
                else if (item instanceof SubscriptionCompleteCommand) {
                    handleSubscriptionCompleteCommand((SubscriptionCompleteCommand) item);
                }
                else if (item instanceof BacklogCommand) {
                    handleBacklogCommand((BacklogCommand) item);
                }
                else if (item instanceof SignalCommand) {
                    handleSignalCommand((SignalCommand) item);
                }
                else if (item instanceof PresenceCommand) {
                    handlePresenceCommand((PresenceCommand) item);
                }
                else if (item instanceof SignalVerificationCommand) {
                    handleSignalVerificationCommand((SignalVerificationCommand) item);
                }
                else if (item instanceof NoopCommand) {
                    handleNoopCommand((NoopCommand) item);
                }
                else {
                    logger.warn("Unrecognised command: " + item.toString());
                }
            }
        });

        connection.onConnectionStateChanged(new Observer<Boolean>() {
            /**
             * The NettySignalConnection will call this method when the connection
             * state has changed.
             *
             * @param sender
             *        The sender might not be the same object every time, so
             *        we'll let it just be object, rather than generics.
             * @param item
             *        True, if connected, False if disconnected
             */
            @Override
            public void notify(Object sender, Boolean item) {
                connectEvent.notifyObservers(sender, item);
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
    public void setClientId(String clientId) {
        this.clientId = clientId;
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
    public Future<Boolean> connect() throws Exception {
        return connect(originalClientId, null);
    }

    @Override
    public Future<Boolean> connect(String clientId) throws Exception {
        return connect(clientId, null);
    }

    @Override
    public Future<Boolean> connect(Map<String, Long> versions) throws Exception {
        return connect(originalClientId, versions);
    }

    @Override
    public Future<Boolean> connect(final String clientId, final Map<String, Long> versions) throws Exception {

        if (isConnected()) {
            logger.debug("Connect requested but already connected");
            return new FakeFuture<Boolean>(true);
        }

        // keep track of the original one, so we can detect change
        originalClientId = clientId;

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
                        logger.error("Error disconnecting", e);
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

        if (command.getVersion() != null && command.getVersion().getValue() >= 0) {
            newVersionEvent.notifyObservers(this, command.getVersion());
        }

        signalEvent.notifyObservers(this, Collections.singletonList(command.getSignal()));
    }

    private void handleSubscriptionCompleteCommand(SubscriptionCompleteCommand command) {

        logger.debug("Handling SubscriptionCompleteCommand");

        if (command.getVersion() != null && command.getVersion().getValue() >= 0) {
            newVersionEvent.notifyObservers(this, command.getVersion());
        }

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

    private void handleNoopCommand(NoopCommand command) {
        logger.debug("Handling NoopCommand");
        //NOOP - for now we are just logging this
    }

}
