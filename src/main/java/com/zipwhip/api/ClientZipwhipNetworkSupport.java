package com.zipwhip.api;

import com.zipwhip.api.exception.NotAuthenticatedException;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.settings.PreferencesSettingsStore;
import com.zipwhip.api.settings.SettingsStore;
import com.zipwhip.api.settings.SettingsVersionStore;
import com.zipwhip.api.settings.VersionStore;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.TearDownConnectionObserver;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionHandleAware;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.concurrent.*;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.DebuggingExecutor;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.zipwhip.concurrent.ThreadUtil.ensureLock;

/**
 * A base class for future implementation to extend.
 * <p/>
 * It takes all the non-API specific stuff out of ZipwhipClient implementations.
 * <p/>
 * If some class wants to communicate with Zipwhip, then it needs to extend this
 * class. This class gives functionality that can be used to parse Zipwhip API.
 * This naming convention was copied from Spring (JmsSupport) base class.
 */
public abstract class ClientZipwhipNetworkSupport extends ZipwhipNetworkSupport {

    protected static final Logger LOGGER = Logger.getLogger(ClientZipwhipNetworkSupport.class);

    protected ImportantTaskExecutor importantTaskExecutor;
    protected long signalsConnectTimeoutInSeconds = 10;

    protected SignalProvider signalProvider;
    protected SettingsStore settingsStore = new PreferencesSettingsStore();
    protected VersionStore versionsStore = new SettingsVersionStore(settingsStore);

    // this is so we can block until SubscriptionCompleteCommand comes in.
    // do we have a current SubscriptionCompleteCommand to use.
    protected ObservableFuture connectFuture;

    private Executor executor = new DebuggingExecutor(Executors.newSingleThreadExecutor(new NamedThreadFactory("ZipwhipClient-")));

    public ClientZipwhipNetworkSupport(ApiConnection connection, SignalProvider signalProvider) {
        super(connection);

        if (signalProvider != null) {
            setSignalProvider(signalProvider);
            link(signalProvider);
        }

        // Start listening to provider events that interest us
        initSignalProviderEvents();
    }

    public ObservableFuture connect() throws Exception {
        return connect(null);
    }

    public synchronized ObservableFuture<ConnectionHandle> connect(final Presence presence) throws Exception {
        synchronized (signalProvider) {
            final ObservableFuture<ConnectionHandle> finalConnectFuture = getUnchangingConnectFuture();
            if (finalConnectFuture != null) {
                LOGGER.debug(String.format("Returning %s since it's still active", connectFuture));
                return finalConnectFuture;
            }

            // throw any necessary sanity checks.
            validateConnectState();

            synchronized (settingsStore) {
                // put the state into the local store
                accessSettings();
                setupSettingsStoreForConnection();

                // pull the state out.
                boolean expectingSubscriptionCompleteCommand = Boolean.parseBoolean(settingsStore.get(SettingsStore.Keys.EXPECTS_SUBSCRIPTION_COMPLETE));
                String clientId = settingsStore.get(SettingsStore.Keys.CLIENT_ID);
                String sessionKey = settingsStore.get(SettingsStore.Keys.SESSION_KEY);
                Map<String, Long> versions = versionsStore.get();

                // this future updates itself (clearing out this.connectFuture)
                final NestedObservableFuture<ConnectionHandle> future = new NestedObservableFuture<ConnectionHandle>(this);
                synchronized (future) {
                    // setting this will cause the other threads to notice that we're connecting.
                    // set it FIRST in case the below line finishes too early! (aka: synchronously!)
                    setConnectFuture(future);

                    ObservableFuture<ConnectionHandle> requestFuture = executeConnectWithFailureDetection(clientId, sessionKey, presence, versions, expectingSubscriptionCompleteCommand);

                    requestFuture.addObserver(
                            new OnlyRunIfSuccessfulObserverAdapter<ConnectionHandle>(
                                    new ThreadSafeObserver<ObservableFuture<ConnectionHandle>>(
                                            // make sure this is still the active connectionHandle.
                                            new ConnectionHandleStillActiveObserverAdapter<ObservableFuture<ConnectionHandle>>(
                                                    new UpdateLocalStoreWithLastKnownSubscribedClientIdOnSuccessObserver(this)))));

                    requestFuture.addObserver(
                            new OnlyRunIfNotSuccessfulObserverAdapter<ConnectionHandle>(
                                    new ThreadSafeObserver<ObservableFuture<ConnectionHandle>>(
                                            new TearDownConnectionObserver<ConnectionHandle>(false))));

                    requestFuture.addObserver(
                            new ThreadSafeObserver<ObservableFuture<ConnectionHandle>>(
                                    new ClearConnectFutureOnCompleteObserver(future)));


                    // need to only alert success from the executor thread.
                    requestFuture.addObserver(new CopyFutureStatusToNestedFuture<ConnectionHandle>(future));

                    return future;
                }
            }
        }
    }

    private ObservableFuture<ConnectionHandle> executeConnectWithFailureDetection(String clientId, String sessionKey, Presence presence, Map<String, Long> versions, boolean expectingSubscriptionCompleteCommand) {
        ObservableFuture<ConnectionHandle> requestFuture = importantTaskExecutor.enqueue(
                executor,
                new ConnectViaSignalProviderTask(this, signalProvider, clientId, sessionKey, presence, versions, expectingSubscriptionCompleteCommand),
                getSignalsConnectTimeoutInSeconds() * 2);


        return requestFuture;
    }

    public synchronized ObservableFuture<ConnectionHandle> disconnect() {
        return disconnect(false);
    }

    public synchronized ObservableFuture<ConnectionHandle> disconnect(final boolean causedByNetwork) {
//        validateConnectState();

        return signalProvider.disconnect(causedByNetwork);
    }

    /**
     * Tells you if this connection is 100% ready to go.
     * <p/>
     * Within the context of ZipwhipClient, we have defined the "connected" state
     * to mean both having a TCP connection AND having a SubscriptionComplete. (ie: connected & authenticated)
     * <p/>
     * Internally we shouldn't use this method because we don't control
     *
     * @return
     */
    public boolean isConnected() {
        ConnectionState connectionState = signalProvider.getConnectionState();

        switch (connectionState) {
            case CONNECTING:
            case CONNECTED:
            case DISCONNECTING:
            case DISCONNECTED:
                return false;
            case AUTHENTICATED:
                // the SignalProvider says that they are AUTHENTICATED. That just means that they have
                // received the {action:CONNECT} command back. We on the other hand need to receive
                // a SubscriptionCompleteCommand. The best way to do that is to check that the current
                // clientId is in our local database as "subscribed"
                synchronized (settingsStore) {
                    String clientId = signalProvider.getClientId();
                    String savedClientId = this.settingsStore.get(SettingsStore.Keys.CLIENT_ID);
                    String lastSubscribedClientId = this.settingsStore.get(SettingsStore.Keys.LAST_SUBSCRIBED_CLIENT_ID);
                    if (StringUtil.equals(clientId, savedClientId)) {
                        // it's current!
                        if (StringUtil.equals(clientId, lastSubscribedClientId)) {
                            // we're up to date!
                            return true;
                        }
                    }

                    return false;
                }
        }

        return false;
    }

    protected void initSignalProviderEvents() {
        signalProvider.getVersionChangedEvent().addObserver(
                new DifferentExecutorObserverAdapter<VersionMapEntry>(executor,
                        new ThreadSafeObserver<VersionMapEntry>(
                                new ConnectionHandleStillActiveObserverAdapter<VersionMapEntry>(
                                        updateVersionsStoreOnVersionChanged))));

        // We don't need to do this because we do our own checking in the ConnectionChangedEvent.
        // Don't trust the SignalProvider to know what the word "new" is within the context of a clientId.
//        signalProvider.getNewClientIdReceivedEvent().addObserver(
//                new DifferentExecutorObserverAdapter<String>(executor,
//                        new ThreadSafeObserver<String>(
//                                new ActiveConnectionHandleFilter<String>(
//                                        onNewClientIdReceivedObserver))));

//        signalProvider.getSubscriptionCompleteReceivedEvent().addObserver(
//                new DifferentExecutorObserverAdapter<SubscriptionCompleteCommand>(executor,
//                        new ThreadSafeObserver<SubscriptionCompleteCommand>(
//                                new ActiveConnectionHandleFilter<SubscriptionCompleteCommand>(
//                                        new Observer<SubscriptionCompleteCommand>() {
//                                            @Override
//                                            public void notify(Object sender, SubscriptionCompleteCommand command) {
//                                                LOGGER.warn(sender);
//                                            }
//                                        }))));

        signalProvider.getConnectionChangedEvent().addObserver(
                new DifferentExecutorObserverAdapter<Boolean>(executor,
                        new ThreadSafeObserver<Boolean>(
                                new Observer<Boolean>() {
                                    @Override
                                    public void notify(Object sender, Boolean connected) {
                                        if (connected) {
                                            ObservableFuture<ConnectionHandle> connectFuture = getUnchangingConnectFuture();
                                            if (connectFuture != null && !connectFuture.isDone()) {
                                                LOGGER.debug("SignalProvider.onConnectionChanged: Connect future is processing, we're going to ignore this one.");
                                                return;
                                            }

                                            final ConnectionHandle connectionHandle = (ConnectionHandle) sender;

                                            String sessionKey = settingsStore.get(SettingsStore.Keys.SESSION_KEY);
                                            String lastSuccessfulClientId = settingsStore.get(SettingsStore.Keys.LAST_SUBSCRIBED_CLIENT_ID);
                                            final String currentClientId = signalProvider.getClientId();

                                            LOGGER.warn(String.format("SignalProvider.onConnectionChanged: lastSuccessfulClientId: %s; currentClientId: %s", lastSuccessfulClientId, currentClientId));

                                            if (!StringUtil.equals(lastSuccessfulClientId, currentClientId)) {
                                                LOGGER.warn("We should ask for a SubscriptionComplete now!");

                                                final ObservableFuture<SubscriptionCompleteCommand> future = executeSignalsConnect(connectionHandle, currentClientId, sessionKey);

                                                future.addObserver(
                                                        new ThreadSafeObserver<ObservableFuture<SubscriptionCompleteCommand>>(
                                                                new Observer<ObservableFuture<SubscriptionCompleteCommand>>() {
                                                                    @Override
                                                                    public void notify(Object sender, ObservableFuture<SubscriptionCompleteCommand> future) {
                                                                        synchronized (future) {
                                                                            if (!future.isSuccess()) {
                                                                                LOGGER.error("UpdateLocalStoreObserver: The future was cancelled or errored. Quitting: " + connectionHandle);
                                                                                return; // this covers isCancelled.
                                                                            }
                                                                        }

                                                                        synchronized (connectionHandle) {
                                                                            if (connectionHandle.isDestroyed()) {
                                                                                LOGGER.error("UpdateLocalStoreObserver: The connectionHandle wasn't active anymore. This must have been for a previous connection. Quitting: " + connectionHandle);
                                                                                return;
                                                                            }

                                                                            LOGGER.debug("UpdateLocalStoreObserver: saving data " + currentClientId);
                                                                            onSubscriptionComplete(currentClientId);
                                                                        }
                                                                    }
                                                                }));

                                                future.addObserver(
                                                        new ThreadSafeObserver<ObservableFuture<SubscriptionCompleteCommand>>(
                                                                new TearDownConnectionObserver<SubscriptionCompleteCommand>(true)));
                                            }
                                        }
                                    }
                                }
                        )));


    }

    private void setupSettingsStoreForConnection() {
        synchronized (settingsStore) {
            /**
             * Validate SESSION KEYS
             */
            boolean cleared = false;
            boolean expectingSubscriptionCompleteCommand = false;

            String existingSessionKey = connection.getSessionKey();
            String existingClientId = signalProvider == null ? null : signalProvider.getClientId();

            String storedSessionKey = settingsStore.get(SettingsStore.Keys.SESSION_KEY);
            String storedClientId = settingsStore.get(SettingsStore.Keys.CLIENT_ID);

            String correctSessionKey = storedSessionKey;
            String correctClientId = existingClientId;

            /**
             * If the sessionKey has changed we need to invalidate the settings data
             */
            if (StringUtil.exists(existingSessionKey) && !StringUtil.equals(existingSessionKey, storedSessionKey)) {
                expectingSubscriptionCompleteCommand = true;
                LOGGER.debug("New or changed sessionKey, resetting session key in settings store");

                cleared = true;
                settingsStore.clear();

                correctSessionKey = existingSessionKey;
            }

            /**
             * If the clientId has changed we need to invalidate the settings data
             */
            if (StringUtil.isNullOrEmpty(storedClientId) || (StringUtil.exists(existingClientId) && !StringUtil.equals(storedClientId, existingClientId))) {
                expectingSubscriptionCompleteCommand = true;
                LOGGER.debug("ClientId has changed, resetting client id in settings store");

                cleared = true;
                settingsStore.clear();

                correctClientId = existingClientId;
            }

            if (cleared) {
                settingsStore.put(SettingsStore.Keys.CLIENT_ID, correctClientId);
                settingsStore.put(SettingsStore.Keys.SESSION_KEY, correctSessionKey);
            }

            // always put this one in
            settingsStore.put(SettingsStore.Keys.EXPECTS_SUBSCRIPTION_COMPLETE, String.valueOf(expectingSubscriptionCompleteCommand));
        }
    }

    private void validateConnectState() throws Exception {
        // if we are already connecting, don't do another connect.
        // we need to determine if we're authenticated enough
        if (!connection.isConnected() || !connection.isAuthenticated()) {
            throw new NotAuthenticatedException("The connection cannot operate at this time");
        }
    }

    private final Observer<VersionMapEntry> updateVersionsStoreOnVersionChanged = new Observer<VersionMapEntry>() {

        @Override
        public void notify(Object sender, VersionMapEntry item) {
            versionsStore.set(item.getKey(), item.getValue());
        }
    };

    private void accessConnectingFuture() {
        ensureLock(signalProvider);
    }

    private void modifyConnectingFuture(ObservableFuture<ConnectionHandle> future) {
        accessConnectingFuture();
        ensureLock(connectFuture);
        ensureLock(future);

        Asserts.assertTrue(connectFuture == null || future == connectFuture, "");
    }

    private void clearConnectingFuture(ObservableFuture<ConnectionHandle> future) {
        modifyConnectingFuture(future);

        connectFuture = null;
    }

    private void setConnectFuture(ObservableFuture<ConnectionHandle> future) {
        accessConnectingFuture();
        modifyConnectingFuture(future);

        if (future == null) {
            throw new RuntimeException("Use clearConnectingFuture() instead");
        }

        if (connectFuture != null) {
            throw new RuntimeException("The connectFuture was not null. You have to clear it first");
        }

        connectFuture = future;
    }

    private ObservableFuture<ConnectionHandle> getUnchangingConnectFuture() {
        accessConnectingFuture();
        return connectFuture;
    }

    public SettingsStore getSettingsStore() {
        return settingsStore;
    }

    public void setSettingsStore(SettingsStore store) {
        this.settingsStore = store;
        this.versionsStore = new SettingsVersionStore(store);
    }

    public SignalProvider getSignalProvider() {
        return signalProvider;
    }

    public void setSignalProvider(SignalProvider signalProvider) {
        this.signalProvider = signalProvider;
    }

    public ImportantTaskExecutor getImportantTaskExecutor() {
        return importantTaskExecutor;
    }

    public void setImportantTaskExecutor(ImportantTaskExecutor importantTaskExecutor) {
        this.importantTaskExecutor = importantTaskExecutor;
    }

    public long getSignalsConnectTimeoutInSeconds() {
        return signalsConnectTimeoutInSeconds;
    }

    public void setSignalsConnectTimeoutInSeconds(long signalsConnectTimeoutInSeconds) {
        this.signalsConnectTimeoutInSeconds = signalsConnectTimeoutInSeconds;
    }

    /**
     * This class lets us 100% guarantee that requests won't overlap/criss-cross. We are able to capture the
     * incoming clientId/sessionKey/connectionHandle and ensure that it doesn't change while we're processing
     * this multi-staged process. We'll do a number of "ConnectionHandle" verification steps to ensure
     * that we're reacting to events that we need.
     * <p/>
     * This Task will execute a signalProvider.connect() and IF NEEDED queue up a /signals/connect request. If
     * we don't expect to receive a SubscriptionCompleteCommand then we'll just quit early.
     * <p/>
     * The caller must decide what to do if this request fails. This class does not do any reconnect or error
     * handling if timeouts occur.
     * <p/>
     * This task _IS_ allowed to change the global state. Most tasks were designed to not change the parent state.
     */
    private static class ConnectViaSignalProviderTask extends DestroyableBase implements Callable<ObservableFuture<ConnectionHandle>>, ConnectionHandleAware {

        final ClientZipwhipNetworkSupport client;
        final SignalProvider signalProvider;
        final String clientId;
        final String sessionKey;
        final boolean expectingSubscriptionCompleteCommand;
        final Presence presence;
        final Map<String, Long> versions;

        ConnectionHandle signalsConnectionHandle;

        // this is the resultingFuture that we send back to the caller.
        // the caller will "nest" this with the "connectFuture"
        final ObservableFuture<ConnectionHandle> resultFuture;

        private boolean started = false;

        private ConnectViaSignalProviderTask(ClientZipwhipNetworkSupport client, SignalProvider signalProvider, String clientId, String sessionKey, Presence presence, Map<String, Long> versions, boolean expectingSubscriptionCompleteCommand) {
            this.client = client;
            this.signalProvider = signalProvider;
            this.clientId = clientId;
            this.sessionKey = sessionKey;
            this.expectingSubscriptionCompleteCommand = expectingSubscriptionCompleteCommand;
            this.presence = presence;
            this.versions = versions;
            this.resultFuture = new DefaultObservableFuture<ConnectionHandle>(this) {
                @Override
                public String toString() {
                    return "[ConnectViaSignalProviderTask: " + super.toString();
                }
            };
        }

        @Override
        public synchronized ObservableFuture<ConnectionHandle> call() throws Exception {
            try {
                // do some basic assertions to ensure sanity checks. (did it change while we waited to execute?)
                validateState();
            } catch (Exception e) {
                return new FakeFailingObservableFuture<ConnectionHandle>(this, e);
            }

            // When resultFuture completes, we need to tear down the global observers that we added.
            unbindGlobalEventsOnComplete(resultFuture);

            bindGlobalEvents();
            ObservableFuture<ConnectionHandle> requestFuture;
            try {
                started = true;
                requestFuture = signalProvider.connect(clientId, versions, presence);
            } catch (Exception e) {
                resultFuture.setFailure(e);

                // this future will be linked to the "connectFuture" correctly.
                return resultFuture;
            }

            /**
             * The finish conditions are 2 fold.
             *
             * == Are we expecting a subscriptionCompleteCommand?
             *
             * 1. If so, we finish when we receive it, or time out.
             * 2. If not, we finish when the {action:CONNECT} comes back.
             */
            attachFinishingEvents(requestFuture);

            selfDestructOnComplete();

            return resultFuture;
        }

        private void selfDestructOnComplete() {
            resultFuture.addObserver(new DestroyOnComplete<ObservableFuture<ConnectionHandle>>(ConnectViaSignalProviderTask.this));
        }

        /**
         * The finish conditions are 2 fold.
         * <p/>
         * == Are we expecting a subscriptionCompleteCommand?
         * <p/>
         * 1. If so, we finish when we receive it, or time out.
         * 2. If not, we finish when the {action:CONNECT} comes back.
         */
        private void attachFinishingEvents(ObservableFuture<ConnectionHandle> requestFuture) {
            LOGGER.debug("Expecting subscriptionComplete: " + expectingSubscriptionCompleteCommand);

            requestFuture.addObserver(onSignalProviderConnectCompleteObserver);

            if (expectingSubscriptionCompleteCommand) {
                // only run the "reset connectFuture" observable if this initial internet request fails
                // (timeout, socket exception, etc).
                // If requestFuture is successful, we still need to wait for the SubscriptionCompleteCommand
                // to come back.
                requestFuture.addObserver(
                        new OnlyRunIfNotSuccessfulObserverAdapter<ConnectionHandle>(
                                new CopyFutureStatusToNestedFuture<ConnectionHandle>(resultFuture)));

                // if this "requestFuture" succeeds, we need to let the onSubscriptionCompleteCommand finish
                // the "connectFuture". That's because we need to wait for the SubscriptionCompleteCommand.
            } else {
                requestFuture.addObserver(new CopyFutureStatusToNestedFuture<ConnectionHandle>(resultFuture));
            }
        }

        private synchronized void unbindGlobalEventsOnComplete(ObservableFuture<ConnectionHandle> future) {
            future.addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
                @Override
                public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                    if (!started) {
                        LOGGER.error("It seems that our future completed without us starting. Is this possible? Did someone cancel something?");
                        return;
                    }

                    unbindGlobalEvents();
                }
            });
        }

        private synchronized void bindGlobalEvents() {
            // NOTE: We have to protect against it calling too soon.
            // we need to add our observers early (in case connect() is synchronous or too fast!)
            // what if we addObserver here, and then between this line and the next it gets called.
            signalProvider.getConnectionChangedEvent().addObserver(failConnectingFutureIfDisconnectedObserver);
        }

        private synchronized void unbindGlobalEvents() {
            signalProvider.getConnectionChangedEvent().removeObserver(failConnectingFutureIfDisconnectedObserver);
        }

        private final Observer<ObservableFuture<ConnectionHandle>> onSignalProviderConnectCompleteObserver = new Observer<ObservableFuture<ConnectionHandle>>() {

            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> requestFuture) {
                LOGGER.debug("onSignalProviderConnectCompleteObserver.notify()");

                synchronized (resultFuture) {
                    if (resultFuture.isCancelled()) {
                        LOGGER.error("onSignalProviderConnectCompleteObserver: The resultFuture was cancelled. So we're going to quit.");
                        return;
                    }

                    Asserts.assertTrue(!resultFuture.isDone(), "The resultFuture should not be done!");

                    synchronized (requestFuture) {
                        if (!requestFuture.isSuccess()) { // this also covers isCancelled()
                            LOGGER.error("onSignalProviderConnectCompleteObserver: The requestFuture was cancelled or failed.");
                            NestedObservableFuture.syncState(requestFuture, resultFuture, null);
                            return;
                        }

                        LOGGER.debug("onSignalProviderConnectCompleteObserver: requestFuture was successful. We now have a connectionHandle.");
                        final ConnectionHandle signalProviderConnectionHandle = ConnectViaSignalProviderTask.this.signalsConnectionHandle = requestFuture.getResult();
                        Asserts.assertTrue(signalProviderConnectionHandle != null, "This can never be null");

                        if (!expectingSubscriptionCompleteCommand) {
                            NestedObservableFuture.syncState(requestFuture, resultFuture, signalProviderConnectionHandle);
                            return;
                        }
                        synchronized (signalsConnectionHandle) {
                            if (signalsConnectionHandle.isDestroyed()) {
                                LOGGER.warn("The connectionHandle was destroyed, so it must not be active.");
                                // The other listeners should have already failed this, right?
                                resultFuture.setFailure(new IllegalStateException("The connectionHandle was destroyed"));
                                return;
                            }

                            // NOTE: We guarantee that this clientId is the exact same we started with because our connectionHandle is still active!!
                            String clientId = signalProvider.getClientId();

                            ObservableFuture<SubscriptionCompleteCommand> signalsConnectFuture = client.executeSignalsConnect(signalProviderConnectionHandle, clientId, sessionKey);

                            // when done, copy it over.
                            signalsConnectFuture.addObserver(
                                    new CopyFutureStatusToNestedFutureWithCustomResult<SubscriptionCompleteCommand, ConnectionHandle>(
                                            resultFuture, signalProviderConnectionHandle));
                        }
                    }
                }
            }

            @Override
            public String toString() {
                return "[ConnectViaSignalProvider/ProcessSignalProvider.connect()]";
            }
        };

        private final Observer<Boolean> failConnectingFutureIfDisconnectedObserver = new Observer<Boolean>() {

            boolean fired = false;

            @Override
            public synchronized void notify(Object sender, Boolean connected) {
                if (!started || isDestroyed()) {
                    // this fired too fast. It's not for our request! Ignore it.
                    LOGGER.warn(String.format("failConnectingFutureIfDisconnectedObserver skipping call. requestFuture:%s/isDestroyed:%s", started, isDestroyed()));
                    return;
                }

                // if not connected, null out the parent future.
                // NOTE: We need to do this the hard way because we need to ensure the right connection got killed.
                if (!connected) {
                    signalProvider.getConnectionChangedEvent().removeObserver(this);

                    Asserts.assertTrue(!fired, "failConnectingFutureIfDisconnectedObserver fired twice. That's not allowed.");
                    fired = true;

                    resultFuture.setFailure(new Exception("Disconnected"));
                }
            }

            @Override
            public String toString() {
                return "failConnectingFutureIfDisconnectedObserver";
            }
        };

        private void validateState() {
            // we are executing either synchronously (we already have the lock) or async in the core executor.
            // either way it's proper to sync on "this"
            // this is the proper order of sync [client->provider]
            synchronized (signalProvider) {
                Asserts.assertTrue(signalProvider.getConnectionState() != ConnectionState.CONNECTED, "Order of operations failure! Already connected!");
                Asserts.assertTrue(signalProvider.getConnectionState() != ConnectionState.AUTHENTICATED, "Order of operations failure! Already authenticated!");
            }
        }

        @Override
        public String toString() {
            return "ConnectViaSignalProviderTask";
        }

        @Override
        protected void onDestroy() {
            LOGGER.debug(this.getClass().toString() + " destroyed.");
        }

        public ConnectionHandle getConnectionHandle() {
            return signalsConnectionHandle;
        }
    }

//    private final Observer<String> onNewClientIdReceivedObserver = new Observer<String>() {
//
//        @Override
//        public synchronized void notify(Object sender, String newClientId) {
//            final ObservableFuture<ConnectionHandle> finalConnectFuture = getUnchangingConnectFuture();
//            if (finalConnectFuture == null || !finalConnectFuture.isDone()) {
//                LOGGER.debug("Got a newClientId while connecting. Ignoring this one.");
//                return;
//            }
//
//            ConnectionHandle connectionHandle = (ConnectionHandle) sender;
//
//            String clientId = settingsStore.get(SettingsStore.Keys.CLIENT_ID);
//            String sessionKey = settingsStore.get(SettingsStore.Keys.SESSION_KEY);
//
//            processNewClientId(connectionHandle, sessionKey, clientId, newClientId);
//        }
//    };

//    private ObservableFuture<SubscriptionCompleteCommand> processNewClientId(ConnectionHandle connectionHandle, String sessionKey, String oldClientId, String newClientId) {
//        // NOTE: when we do a reset, we're going to get a new clientId that is null
////                if (StringUtil.isNullOrEmpty(newClientId)) {
////                    settingsStore.put(SettingsStore.Keys.CLIENT_ID, newClientId);
////                    return;
////                }
//
//        final ObservableFuture<SubscriptionCompleteCommand> signalsConnectFuture;
//
//        if (StringUtil.exists(oldClientId)) {
//            // clientId changed, unsubscribe the old one, and sub the new one
//            if (!oldClientId.equals(newClientId)) {
//                synchronized (settingsStore) {
//                    accessSettings();
//                    settingsStore.clear();
//
//                    settingsStore.put(SettingsStore.Keys.SESSION_KEY, sessionKey);
//                    settingsStore.put(SettingsStore.Keys.CLIENT_ID, newClientId);
//                    settingsStore.put(SettingsStore.Keys.EXPECTS_SUBSCRIPTION_COMPLETE, "true");
//
//                    executeSignalsDisconnect(sessionKey, oldClientId);
//
//                    signalsConnectFuture = executeSignalsConnect(connectionHandle, newClientId, sessionKey);
//                }
//            } else {
//                signalsConnectFuture = null;
//                // just the same clientId
//            }
//        } else {
//            synchronized (settingsStore) {
//                accessSettings();
//
//                settingsStore.put(SettingsStore.Keys.CLIENT_ID, newClientId);
//                settingsStore.put(SettingsStore.Keys.EXPECTS_SUBSCRIPTION_COMPLETE, "true");
//
//                signalsConnectFuture = executeSignalsConnect(connectionHandle, newClientId, sessionKey);
//
//                signalsConnectFuture.addObserver(new Observer<ObservableFuture<SubscriptionCompleteCommand>>() {
//                    @Override
//                    public void notify(Object sender, ObservableFuture<SubscriptionCompleteCommand> item) {
//                        new UpdateLocalStoreWithLastKnownSubscribedClientIdOnSuccessObserver(this).no;
//                    }
//                });
//            }
//        }
//
//        return signalsConnectFuture;
//    }


    private static class ConnectionHandleStillActiveObserverAdapter<T> extends ObserverAdapter<T> {

        private ConnectionHandleStillActiveObserverAdapter(Observer<T> observer) {
            super(observer);
        }

        @Override
        public void notify(Object sender, T item) {
            ConnectionHandle connectionHandle = getConnectionHandle(sender, item);
            if (connectionHandle == null) {
                LOGGER.error("The connectionHandle passed in was null. Was this a bad disconnect? Quitting " + getObserver());
                return;
            }

            synchronized (connectionHandle) {
                if (connectionHandle.isDestroyed() || connectionHandle.getDisconnectFuture().isDone()) {
                    LOGGER.error("The connectionHandle was destroyed. Was this connection torn down?");
                    return;
                }

                super.notify(sender, item);
            }
        }

        protected ConnectionHandle getConnectionHandle(Object sender, T item) {
            if (sender instanceof ConnectionHandle) {
                return (ConnectionHandle) sender;
            } else if (sender instanceof ConnectionHandleAware) {
                return ((ConnectionHandleAware) sender).getConnectionHandle();
            } else if (item instanceof ConnectionHandle) {
                return (ConnectionHandle) item;
            } else {
                throw new IllegalArgumentException("Cannot find connectionHandle on sender");
            }
        }
    }

    /**
     * Execute a /signals/connect webcall s
     *
     * @param clientId
     * @param sessionKey
     * @return
     */
    private ObservableFuture<SubscriptionCompleteCommand> executeSignalsConnect(ConnectionHandle connectionHandle, final String clientId, final String sessionKey) {
//        asdfasdf // An "ImportantTask" is never allowed to modify the state of "this". Just return the future and let the caller decide what to do on success/failure.

        /**
         *
         * This is a call that will time out. We had to do the "importantTaskExecutor" in order to allow Android AlarmManager
         * to run the scheduling.
         */
        final ObservableFuture<SubscriptionCompleteCommand> signalsConnectFuture =
                importantTaskExecutor.enqueue(executor, new SignalsConnectTask(connectionHandle, sessionKey, clientId), signalsConnectTimeoutInSeconds);

        signalsConnectFuture.addObserver(new DebugObserver<SubscriptionCompleteCommand>());
        signalsConnectFuture.addObserver(new Observer<ObservableFuture<SubscriptionCompleteCommand>>() {
            @Override
            public void notify(Object sender, ObservableFuture<SubscriptionCompleteCommand> item) {
                LOGGER.debug("/signals/connect >> Success:" + item.isSuccess());
            }
        });

        return signalsConnectFuture;
    }

    private void executeSignalsDisconnect(String sessionKey, String clientId) {
        // Do a disconnect then connect
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("clientId", clientId);
        params.put("sessions", sessionKey);
        try {
            executeAsync(SIGNALS_DISCONNECT, params);
        } catch (Exception e) {
            LOGGER.warn("Couldn't execute SIGNALS_DISCONNECT. We're going to ignore this problem.", e);
        }
    }

    /**
     * Will update the settingsStore with the right information when this future completes successfully.
     */
    private static class UpdateLocalStoreWithLastKnownSubscribedClientIdOnSuccessObserver implements Observer<ObservableFuture<ConnectionHandle>> {

        private static final Logger LOGGER = Logger.getLogger(UpdateLocalStoreWithLastKnownSubscribedClientIdOnSuccessObserver.class);

        private final ClientZipwhipNetworkSupport client;

        private UpdateLocalStoreWithLastKnownSubscribedClientIdOnSuccessObserver(ClientZipwhipNetworkSupport client) {
            this.client = client;
        }

        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> future) {
            synchronized (future) {
                if (future.isCancelled()) {
                    LOGGER.error("Future was cancelled. Quitting!");
                    return;
                }

                if (future.isSuccess()) {
                    LOGGER.debug("Successfully updating the settings store with the new information");
                    synchronized (client) {
                        synchronized (client.getSignalProvider()) {
                            synchronized (client.getSettingsStore()) {
                                // the subscriptionId is the sessionKey because we request it as so.
                                ConnectionHandle connectionHandle = future.getResult();
                                synchronized (connectionHandle) {
                                    if (connectionHandle.isDestroyed()) {
                                        LOGGER.warn("The connectionHandle was destroyed, so we're not updating the database.");
                                        return;
                                    }
                                    // do some quick assertions to ensure that we're doing things right.
//                                Asserts.assertTrue(StringUtil.equals(command.getSubscriptionId(), sessionKey), "");
                                    // Just logging for now - we should have gotten the subscriptionId in the SubscriptionCompleteCommand: http://angela.zipwhip.com/issues/7678

                                    String clientId = client.getSignalProvider().getClientId();

                                    client.onSubscriptionComplete(clientId);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private synchronized void onSubscriptionComplete(String clientId) {
        accessSettings();

        settingsStore.put(SettingsStore.Keys.CLIENT_ID, clientId);
        settingsStore.put(SettingsStore.Keys.EXPECTS_SUBSCRIPTION_COMPLETE, "false");
        settingsStore.put(SettingsStore.Keys.LAST_SUBSCRIBED_CLIENT_ID, clientId);
    }

    private void accessSettings() {
        ensureLock(ClientZipwhipNetworkSupport.this);
        ensureLock(signalProvider);
        ensureLock(settingsStore);
    }

    /**
     * This class's job is to do a /signals/connect and wrap the return from the server in one mega Future.
     * <p/>
     * It is NOT allowed to change the state of any parent property.
     */
    private class SignalsConnectTask implements Callable<ObservableFuture<SubscriptionCompleteCommand>>, ConnectionHandleAware {

        private final String sessionKey;
        private final String clientId;

        /**
         * This is the connectionHandle that we are operating on.
         */
        private final ConnectionHandle connectionHandle;

        private SignalsConnectTask(ConnectionHandle connectionHandle, String sessionKey, String clientId) {
            this.sessionKey = sessionKey;
            this.connectionHandle = connectionHandle;
            this.clientId = clientId;
        }

        @Override
        public synchronized ObservableFuture<SubscriptionCompleteCommand> call() throws Exception {

            // it's important that this future is synchronous (no executor)
            final ObservableFuture<SubscriptionCompleteCommand> resultFuture = new DefaultObservableFuture<SubscriptionCompleteCommand>(this);
            final Observer<Boolean>[] onConnectionChangedObserver = new Observer[1];

            final Observer<SubscriptionCompleteCommand> onSubscriptionCompleteObserver = new Observer<SubscriptionCompleteCommand>() {
                @Override
                public void notify(Object sender, SubscriptionCompleteCommand item) {
                    synchronized (SignalsConnectTask.this) {
                        signalProvider.getSubscriptionCompleteReceivedEvent().removeObserver(this);
                        signalProvider.getConnectionChangedEvent().removeObserver(onConnectionChangedObserver[0]);

                        LOGGER.debug("Successing");
                        resultFuture.setSuccess(item);
                    }
                }

                @Override
                public String toString() {
                    return "SignalsConnectTask/onSubscriptionCompleteObserver";
                }
            };

            onConnectionChangedObserver[0] = new Observer<Boolean>() {
                @Override
                public void notify(Object sender, Boolean connected) {
                    synchronized (SignalsConnectTask.this) {
                        if (connected) {
                            // we connected?
                            return;
                        }

                        // on any kind of connection change, we need to just abort
                        signalProvider.getConnectionChangedEvent().removeObserver(onConnectionChangedObserver[0]);
                        signalProvider.getSubscriptionCompleteReceivedEvent().removeObserver(onSubscriptionCompleteObserver);

                        LOGGER.debug("Failing (disconnected)");
                        resultFuture.setFailure(new Exception("Disconnected while waiting for SubscriptionCompleteCommand to come in! " + connected));
                    }
                }

                @Override
                public String toString() {
                    return "SignalsConnectTask/onConnectionChangedObserver";
                }
            };

            signalProvider.getConnectionChangedEvent().addObserver(onConnectionChangedObserver[0]);
            signalProvider.getSubscriptionCompleteReceivedEvent().addObserver(onSubscriptionCompleteObserver);

            if (resultFuture.isDone()) {
                // wow it finished already?
                return resultFuture;
            }

            ServerResponse response;
            try {
                response = executeSync(ZipwhipNetworkSupport.SIGNALS_CONNECT, getSignalsConnectParams(sessionKey, clientId));
            } catch (Exception e) {
                LOGGER.error("Failed to execute request: ", e);
                resultFuture.setFailure(e);

                return resultFuture;
            }

            if (response == null) {
                LOGGER.error("The response from zipwhip was null!?");
                resultFuture.setFailure(new NullPointerException("The executeSync() response was null"));

                return resultFuture;
            }

            if (!response.isSuccess()) {
                resultFuture.setFailure(new Exception(response.getRaw()));

                return resultFuture;
            }

            resultFuture.addObserver(new Observer<ObservableFuture<SubscriptionCompleteCommand>>() {

                @Override
                public void notify(Object sender, ObservableFuture<SubscriptionCompleteCommand> item) {
                    synchronized (SignalsConnectTask.this) {
                        signalProvider.getSubscriptionCompleteReceivedEvent().removeObserver(onSubscriptionCompleteObserver);
                        signalProvider.getConnectionChangedEvent().removeObserver(onConnectionChangedObserver[0]);
                    }
                }

                @Override
                public String toString() {
                    return "SignalsConnectTask/CleanUpObserver";
                }
            });

            LOGGER.debug("/signals/connect executed successfully. You should get back a SubscriptionCompleteCommand any time now. (Maybe already?)");

            return resultFuture;
        }

        private Map<String, Object> getSignalsConnectParams(String sessionKey, String clientId) {
            Map<String, Object> params = new HashMap<String, Object>();

            params.put("sessions", sessionKey);
            params.put("clientId", clientId);
            params.put("subscriptionId", sessionKey);

            if (signalProvider.getPresence() != null) {
                params.put("category", signalProvider.getPresence().getCategory());
            }

            return params;
        }

        public ConnectionHandle getConnectionHandle() {
            return connectionHandle;
        }

        @Override
        public String toString() {
            return "SignalsConnectTask(Waiting for SubscriptionCompleteCommand)";
        }
    }

    /**
     * This observer fires in the thread that the future completes in. This future can either complete in the
     * Timer thread (HashWheelTimer or pubsub via intent/AlarmManager) OR it can
     */
    private class ClearConnectFutureOnCompleteObserver implements Observer<ObservableFuture<ConnectionHandle>> {

        private final ObservableFuture<ConnectionHandle> myConnectFuture;

        private ClearConnectFutureOnCompleteObserver(ObservableFuture<ConnectionHandle> connectFuture) {
            this.myConnectFuture = connectFuture;
        }

        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
            // we are not allowed to "synchronize" on the ZipwhipClient because we are in a bad thread. (causes
            // a deadlock). We can rely on the property that only 1 connectFuture is allowed to exist at any
            // given time. It only is allowed to be filled when not null.
            synchronized (signalProvider) {
                final ObservableFuture<ConnectionHandle> finalConnectingFuture = getUnchangingConnectFuture();
                if (finalConnectingFuture == myConnectFuture) {
                    synchronized (myConnectFuture) {
                        clearConnectingFuture(myConnectFuture);
                    }
                }
            }
        }
    }

    private class ThreadSafeObserver<T> extends ObserverAdapter<T> {

        private ThreadSafeObserver(Observer<T> observer) {
            super(observer);
        }

        @Override
        public void notify(Object sender, T item) {
            synchronized (signalProvider) {
                super.notify(sender, item);
            }
        }
    }
}
