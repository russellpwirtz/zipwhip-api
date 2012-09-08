package com.zipwhip.api;

import com.zipwhip.api.exception.NotAuthenticatedException;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.settings.PreferencesSettingsStore;
import com.zipwhip.api.settings.SettingsStore;
import com.zipwhip.api.settings.SettingsVersionStore;
import com.zipwhip.api.settings.VersionStore;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.FakeFailingObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

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
    protected long signalsConnectTimeoutInSeconds = 30;

    protected SignalProvider signalProvider;
    protected SettingsStore settingsStore = new PreferencesSettingsStore();
    protected VersionStore versionsStore = new SettingsVersionStore(settingsStore);

    // this is so we can block until SubscriptionCompleteCommand comes in.
    // do we have a current SubscriptionCompleteCommand to use.
    protected ObservableFuture connectingFuture;

    public ClientZipwhipNetworkSupport(ApiConnection connection, SignalProvider signalProvider) {
        super(connection);

        if (signalProvider != null) {
            setSignalProvider(signalProvider);
            link(signalProvider);
        }

        // Start listening to provider events that interest us
        initSignalProviderEvents();
    }

    public SignalProvider getSignalProvider() {
        return signalProvider;
    }

    protected void initSignalProviderEvents() {
        signalProvider.getVersionChangedEvent().addObserver(updateVersionsStoreOnVersionChanged);
    }

    /**
     * Tells you if this connection is 100% ready to go.
     * <p/>
     * Within the context of ZipwhipClient, we have defined the "connected" state
     * to mean both having a TCP connection AND having a SubscriptionComplete. (ie: connected & authenticated)
     *
     * @return
     */
    public boolean isConnected() {
        ConnectionState connectionState = signalProvider.getConnectionState();

        switch (connectionState) {
            case CONNECTING:
                return false;
            case CONNECTED:
                return false;
            case DISCONNECTING:
                return false;
            case DISCONNECTED:
                return false;
            case AUTHENTICATED:
                return connectingFuture == null || connectingFuture.isDone();
        }

        return false;
    }

    public ObservableFuture connect() throws Exception {
        return connect(null);
    }

    public synchronized ObservableFuture connect(final Presence presence) throws Exception {
        if (connectingFuture != null) {
            return connectingFuture;
        }

        // throw any necessary sanity checks.
        validateConnectState();

        // put the state into the local store
        setupSettingsStoreForConnection();

        // pull the state out.
        boolean expectingSubscriptionCompleteCommand = Boolean.parseBoolean(settingsStore.get(SettingsStore.Keys.EXPECTS_SUBSCRIPTION_COMPLETE));
        String clientId = settingsStore.get(SettingsStore.Keys.CLIENT_ID);
        String sessionKey = settingsStore.get(SettingsStore.Keys.SESSION_KEY);
        Map<String, Long> versions = versionsStore.get();

        // this future updates itself (clearing out this.connectingFuture)
        final NestedObservableFuture<ConnectionHandle> future = new NestedObservableFuture<ConnectionHandle>(this);

        // setting this will cause the other threads to notice that we're connecting.
        // set it FIRST in case the below line finishes too early! (aka: synchronously!)
        connectingFuture = future;

//        synchronized (signalProvider) {
//            Asserts.assertTrue(!signalProvider.getConnectionState() == ConnectionState.CONNECTED, "How can we connect if we are already connected!");
//        }

        ObservableFuture<ConnectionHandle> requestFuture = importantTaskExecutor.enqueue(
                null,
                new ConnectViaSignalProviderTask(clientId, sessionKey, presence, versions, expectingSubscriptionCompleteCommand),
                getSignalsConnectTimeoutInSeconds());


        requestFuture.addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {

            /**
             * This thread is the "executor" thread because of the constructor for the ObservableFuture
             *
             * @param sender
             * @param item
             */
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                if (item.isSuccess()) {
                    // we are in the SignalConnection thread.
                } else {
                    if (item.getCause() instanceof TimeoutException) {
                        // we are in the HashWheelTimer (or pubsub via intent) thread.
                    }
                }

                // we are not allowed to "synchronize" on the ZipwhipClient because we are in a bad thread. (causes
                // a deadlock). We can rely on the property that only 1 connectingFuture is allowed to exist at any
                // given time. It only is allowed to be filled when not null.
                if (connectingFuture == future) {
                    ClientZipwhipNetworkSupport.this.connectingFuture = null;
                }
            }
        });


        // run the execution now (have to do this AFTER setting connectingFuture in case connection is freakishly fast (ie: sync)).
        future.setNestedFuture(requestFuture);

        return future;
    }

    public synchronized ObservableFuture<ConnectionHandle> disconnect() {
        return disconnect(false);
    }

    public synchronized ObservableFuture<ConnectionHandle> disconnect(final boolean causedByNetwork) {
//        validateConnectState();

        return signalProvider.disconnect(causedByNetwork);
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

    private final Observer<VersionMapEntry> updateVersionsStoreOnVersionChanged = new Observer<VersionMapEntry>() {

        @Override
        public void notify(Object sender, VersionMapEntry item) {
            versionsStore.set(item.getKey(), item.getValue());
        }
    };

//    private final Observer<ObservableFuture<SubscriptionCompleteCommand>> reconnectSignalProviderIfNotSuccessfulObserver = new Observer<ObservableFuture<SubscriptionCompleteCommand>>() {
//
//        @Override
//        public void notify(Object sender, ObservableFuture<SubscriptionCompleteCommand> future) {
//            if (future.isSuccess()) {
//                return;
//            }
//
//            if (isTimeoutException(future)) {
//                // We are in the hashwheel timer.
//                final String sessionKey = connection.getSessionKey();
//                final String clientId = signalProvider.getClientId();
//
//                // we are in the hashwheel timer.
//                signalProvider.runIfActive(new Runnable() {
//                    @Override
//                    public void run() {
//                        LOGGER.debug("signalProvider.runIfActive() hit. We're going to tear down this connection. We can trust that it wont change during this method.");
//                        String s = connection.getSessionKey();
//                        String c = signalProvider.getClientId();
//                        if (!StringUtil.equals(sessionKey, s)) {
//                            LOGGER.warn(String.format("The sessionKey changed from underneath us. [%s->%s] Just quitting", sessionKey, s));
//                            return;
//                        } else if (!StringUtil.equals(clientId, c)) {
//                            LOGGER.warn(String.format("The clientId changed from underneath us. [%s->%s] Just quitting", clientId, c));
//                            return;
//                        }
//
//                        try {
//                            LOGGER.debug("We're safely in the same connection as we were before, so we're going to tear down the connection since we missed a SubscriptionCompleteCommand");
//                            signalProvider.resetDisconnectAndConnect();
//                        } catch (Exception e) {
//                            LOGGER.error("");
//                        }
//                    }
//                });
//            }
//
//        }
//
//        private boolean isTimeoutException(ObservableFuture<SubscriptionCompleteCommand> future) {
//            return future.getCause() instanceof TimeoutException;
//        }
//    };

    /**
     * This future will auto
     *
     * @return
     */
    private NestedObservableFuture<Boolean> createSelfUpdatingConnectingFuture() {
        final NestedObservableFuture<Boolean> result = new NestedObservableFuture<Boolean>(this) {

            @Override
            public String toString() {
                return "[connectingFuture: " + super.toString();    //To change body of overridden methods use File | Settings | File Templates.
            }
        };

        return result;
    }

    /**
     * This class lets us 100% guarantee that requests won't overlap/criss-cross. We are able to capture the incoming
     * clientId/sessionKey/stateId and ensure that it doesn't change while we're processing this multi-staged process.
     */
    private class ConnectViaSignalProviderTask extends DestroyableBase implements Callable<ObservableFuture<ConnectionHandle>> {

        final String clientId;
        final String sessionKey;
        final boolean expectingSubscriptionCompleteCommand;
        final Presence presence;
        final Map<String, Long> versions;

        ConnectionHandle signalsConnectionHandle;

        // this is the resultingFuture that we send back to the caller.
        // the caller will "nest" this with the "connectingFuture"
        final ObservableFuture<ConnectionHandle> resultFuture;

        private boolean started = false;

        /**
         * These values get populated during the "newClientIdReceived" process.
         */
        private boolean newClientIdReceivedWhileWaitingToConnect = false;
        private String newClientIdWhileWaitingToConnect = null;
        private ConnectionHandle signalsConnectionHandleFromNewClientId;

        private ConnectViaSignalProviderTask(String clientId, String sessionKey, Presence presence, Map<String, Long> versions, boolean expectingSubscriptionCompleteCommand) {
            this.clientId = clientId;
            this.sessionKey = sessionKey;
            this.expectingSubscriptionCompleteCommand = expectingSubscriptionCompleteCommand;
            this.presence = presence;
            this.versions = versions;
            this.resultFuture = new DefaultObservableFuture<ConnectionHandle>(ClientZipwhipNetworkSupport.this) {
                @Override
                public String toString() {
                    return "[ConnectViaSignalProviderTask: " + super.toString();
                }
            };
        }

        @Override
        public synchronized ObservableFuture<ConnectionHandle> call() throws Exception {
            // do some basic assertions to ensure sanity checks.
            try {
                validateState();
            } catch (Exception e) {
                return new FakeFailingObservableFuture<ConnectionHandle>(this, e);
            }

            // do some basic assertions to ensure sanity checks. (did it change while we waited to execute?)
            setupGlobalEvents();

            ObservableFuture<ConnectionHandle> requestFuture;
            try {
                started = true;
                requestFuture = signalProvider.connect(clientId, versions, presence);
            } catch (Exception e) {
                resultFuture.setFailure(e);

                // this future will be linked to the "connectingFuture" correctly.
                return resultFuture;
            }

            attachFinishingEvents(requestFuture);

            return resultFuture;
        }

        private void attachFinishingEvents(ObservableFuture<ConnectionHandle> requestFuture) {
            LOGGER.debug("Expecting subscriptionComplete: " + expectingSubscriptionCompleteCommand);


            if (expectingSubscriptionCompleteCommand) {
                // only run the "reset connectingFuture" observable if this initial internet request fails
                // (timeout, socket exception, etc).
                requestFuture.addObserver(
                        new OnlyRunIfFailedObserverAdapter<ConnectionHandle>(
                                new CopyFutureStatusToNestedFuture<ConnectionHandle>(resultFuture)));

                // if this "requestFuture" succeeds, we need to let the onSubscriptionCompleteCommand finish
                // the "connectingFuture". That's because we need to wait for the SubscriptionCompleteCommand.
            } else {
                requestFuture.addObserver(new CopyFutureStatusToNestedFuture<ConnectionHandle>(resultFuture));
            }

            requestFuture.addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
                @Override
                public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                    LOGGER.debug("finishingEvent:requestFuture:done!");

                    if (item.isSuccess()) {
                        final ConnectionHandle connectionHandle = ConnectViaSignalProviderTask.this.signalsConnectionHandle = item.getResult();

                        if (newClientIdReceivedWhileWaitingToConnect) {
                            Asserts.assertTrue(signalsConnectionHandleFromNewClientId == connectionHandle, "The connectionHandles didnt match up!!!!");

                            LOGGER.debug("finishingEvent:requestFuture:done! processing the newClientId that we postponed");
                            // we received it already
                            processNewClientId(connectionHandle, newClientIdWhileWaitingToConnect);
                        }
                    }

                    // destroy the task once this future is complete.
                    ConnectViaSignalProviderTask.this.destroy();
                }
            });
        }

        private void setupGlobalEvents() {
            // when resultFuture completes, we need to tear down the global observers that we added.
            // NOTE: is it possible to finish the "connectingFuture" without also finishing the resultFuture??
            cleanObserversOnComplete(resultFuture);

            // NOTE: We have to protect against it calling too soon.
            // we need to add our observers early (in case connect() is synchronous or too fast!)
            // what if we addObserver here, and then between this line and the next it gets called.
            signalProvider.getConnectionChangedEvent().addObserver(failConnectingFutureIfDisconnectedObserver);

            if (expectingSubscriptionCompleteCommand) {
                signalProvider.getNewClientIdReceivedEvent().addObserver(executeSignalsConnectOnNewClientIdReceived);
            }
        }

        private synchronized void cleanObserversOnComplete(ObservableFuture<ConnectionHandle> future) {
            future.addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
                @Override
                public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                    if (!started) {
                        return;
                    }

                    Asserts.assertTrue(started, "How can this be called before we've even made the request!");

                    signalProvider.getConnectionChangedEvent().removeObserver(failConnectingFutureIfDisconnectedObserver);

                    if (expectingSubscriptionCompleteCommand) {
                        signalProvider.getNewClientIdReceivedEvent().removeObserver(executeSignalsConnectOnNewClientIdReceived);
                    }
                }
            });
        }

        private final Observer<String> executeSignalsConnectOnNewClientIdReceived = new Observer<String>() {

            @Override
            public synchronized void notify(Object sender, String newClientId) {
                if (!started || isDestroyed()) {
                    // this fired too fast, it's not for our request!
                    return;
                }

                ConnectionHandle connectionHandle = (ConnectionHandle) sender;

                /**
                 * TODO: write description of this order-of-operations problem
                 */
                if (ConnectViaSignalProviderTask.this.signalsConnectionHandle == null) {
                    ConnectViaSignalProviderTask.this.newClientIdReceivedWhileWaitingToConnect = true;
                    ConnectViaSignalProviderTask.this.signalsConnectionHandleFromNewClientId = connectionHandle;
                    ConnectViaSignalProviderTask.this.newClientIdWhileWaitingToConnect = newClientId;
                    LOGGER.debug("The newClientId was received too early, so we queued it up for processing later.");

                    // we already handled it. Don't handle it twice!
                    signalProvider.getNewClientIdReceivedEvent().removeObserver(this);
                    return;
                } else if (ConnectViaSignalProviderTask.this.signalsConnectionHandle != connectionHandle) {
                    LOGGER.error("Noticed that the connectionHandles didn't match up. Did we get a request we weren't expecting?");
                    return;
                } else {
                    // we already handled it. Don't handle it twice!
                    signalProvider.getNewClientIdReceivedEvent().removeObserver(this);
                }

                processNewClientId(connectionHandle, newClientId);
            }
        };

        private void processNewClientId(ConnectionHandle connectionHandle, String newClientId) {
            // NOTE: when we do a reset, we're going to get a new clientId that is null
//                if (StringUtil.isNullOrEmpty(newClientId)) {
//                    settingsStore.put(SettingsStore.Keys.CLIENT_ID, newClientId);
//                    return;
//                }

            String oldClientId = settingsStore.get(SettingsStore.Keys.CLIENT_ID);
//                final String sessionKey = connection.getSessionKey();
            final ObservableFuture<SubscriptionCompleteCommand> signalsConnectFuture;

            if (StringUtil.exists(oldClientId)) {

                // clientId changed, unsubscribe the old one, and sub the new one
                if (!oldClientId.equals(newClientId)) {

                    settingsStore.clear();

                    settingsStore.put(SettingsStore.Keys.SESSION_KEY, sessionKey);
                    settingsStore.put(SettingsStore.Keys.CLIENT_ID, newClientId);

                    // Do a disconnect then connect
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put("clientId", oldClientId);
                    params.put("sessions", sessionKey);
                    try {
                        executeAsync(SIGNALS_DISCONNECT, params);
                    } catch (Exception e) {
                        LOGGER.warn("Couldn't execute SIGNALS_DISCONNECT", e);
                    }

                    signalsConnectFuture = executeSignalsConnect(signalsConnectionHandle, newClientId, sessionKey);
                } else {
                    signalsConnectFuture = null;
                    // just the same clientId
                }
            } else {
                settingsStore.put(SettingsStore.Keys.CLIENT_ID, newClientId);

                signalsConnectFuture = executeSignalsConnect(connectionHandle, newClientId, sessionKey);
            }

            if (signalsConnectFuture != null) {
                signalsConnectFuture.addObserver(new TearDownConnectionIfFailureObserver(newClientId, signalProvider));

                // cascade the success/failure/cancellation
                signalsConnectFuture.addObserver(new CascadeStateObserver<ConnectionHandle, SubscriptionCompleteCommand>(resultFuture, signalsConnectionHandle));
            }
        }

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

        /**
         * Execute a /signals/connect webcall s
         *
         * @param clientId
         * @param sessionKey
         * @return
         */
        private ObservableFuture<SubscriptionCompleteCommand> executeSignalsConnect(ConnectionHandle connectionHandle, final String clientId, final String sessionKey) {


            /**
             * This is a call that will time out. We had to do the "importantTaskExecutor" in order to allow Android AlarmManager
             * to run the scheduling.
             */
            final ObservableFuture<SubscriptionCompleteCommand> signalsConnectFuture =
                    importantTaskExecutor.enqueue(null, new SignalsConnectTask(connectionHandle, sessionKey, clientId), signalsConnectTimeoutInSeconds);

            return signalsConnectFuture;
        }

        private void validateState() {
            // we are executing either synchronously (we already have the lock) or async in the core executor.
            // either way it's proper to sync on "this"
            synchronized (ClientZipwhipNetworkSupport.this) {
                // this is the proper order of sync [client->provider]
                synchronized (signalProvider) {
                    Asserts.assertTrue(signalProvider.getConnectionState() != ConnectionState.CONNECTED, "Order of operations failure! Already connected!");
                    Asserts.assertTrue(signalProvider.getConnectionState() != ConnectionState.AUTHENTICATED, "Order of operations failure! Already authenticated!");
                }
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

    }

    /**
     * This class's job is to do a /signals/connect and wrap the return from the server in one mega Future.
     * <p/>
     * It is NOT allowed to change the state of any parent property.
     */
    private class SignalsConnectTask implements Callable<ObservableFuture<SubscriptionCompleteCommand>> {

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
            Map<String, Object> params = new HashMap<String, Object>();

            params.put("sessions", sessionKey);
            params.put("clientId", clientId);

            if (signalProvider.getPresence() != null){
                params.put("category", signalProvider.getPresence().getCategory());
            }

            // it's important that this future is synchronous (no executor)
            final ObservableFuture<SubscriptionCompleteCommand> resultFuture = new DefaultObservableFuture<SubscriptionCompleteCommand>(this);
            final Observer<Boolean>[] onDisconnectObserver = new Observer[1];

            final Observer<SubscriptionCompleteCommand> onSubscriptionCompleteObserver = new Observer<SubscriptionCompleteCommand>() {
                @Override
                public void notify(Object sender, SubscriptionCompleteCommand item) {
                    synchronized (ClientZipwhipNetworkSupport.this) {
                        synchronized (signalProvider) {
                            synchronized (SignalsConnectTask.this) {
                                signalProvider.getSubscriptionCompleteReceivedEvent().removeObserver(this);
                                signalProvider.getConnectionChangedEvent().removeObserver(onDisconnectObserver[0]);

                                LOGGER.debug("Successing");
                                resultFuture.setSuccess(item);
                            }
                        }
                    }
                }

                @Override
                public String toString() {
                    return "SignalsConnectTask/onSubscriptionCompleteObserver";
                }
            };

            onDisconnectObserver[0] = new Observer<Boolean>() {
                @Override
                public void notify(Object sender, Boolean item) {
                    synchronized (ClientZipwhipNetworkSupport.this) {
                        synchronized (signalProvider) {
                            synchronized (SignalsConnectTask.this) {
                                if (item) {
                                    // we connected?
                                    return;
                                }

                                // on any kind of connection change, we need to just abort
                                signalProvider.getConnectionChangedEvent().removeObserver(onDisconnectObserver[0]);
                                signalProvider.getSubscriptionCompleteReceivedEvent().removeObserver(onSubscriptionCompleteObserver);

                                LOGGER.debug("Failing (disconnected)");
                                resultFuture.setFailure(new Exception("Disconnected while waiting for SubscriptionCompleteCommand to come in! " + item));
                            }
                        }
                    }
                }

                @Override
                public String toString() {
                    return "SignalsConnectTask/onDisconnectObserver";
                }
            };

            signalProvider.getConnectionChangedEvent().addObserver(onDisconnectObserver[0]);
            signalProvider.getSubscriptionCompleteReceivedEvent().addObserver(onSubscriptionCompleteObserver);

            if (resultFuture.isDone()) {
                // wow it finished already?
                return resultFuture;
            }

            ServerResponse response;
            try {
                response = executeSync(ZipwhipNetworkSupport.SIGNALS_CONNECT, params);
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
                        signalProvider.getConnectionChangedEvent().removeObserver(onDisconnectObserver[0]);
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

        public ConnectionHandle getConnectionHandle() {
            return connectionHandle;
        }
    }

    /**
     * So we can do conditionals with constructors
     *
     * @param <T>
     */
    private static class OnlyRunIfFailedObserverAdapter<T> implements Observer<ObservableFuture<T>> {

        final Observer<ObservableFuture<T>> observer;

        private OnlyRunIfFailedObserverAdapter(Observer<ObservableFuture<T>> observer) {
            this.observer = observer;
        }

        @Override
        public void notify(Object sender, ObservableFuture<T> item) {
            if (!item.isSuccess()) {
                observer.notify(sender, item);
            } else {
                LOGGER.debug("Did not notify inner observer because successful. " + item);
            }
        }

    }

    private static class CascadeStateObserver<T, TOther> implements Observer<ObservableFuture<TOther>> {

        private final ObservableFuture<T> future;
        private final T result;

        public CascadeStateObserver(ObservableFuture<T> future, T result) {
            this.future = future;
            this.result = result;
        }

        @Override
        public void notify(Object sender, ObservableFuture<TOther> item) {
            if (item.isSuccess()) {
                future.setSuccess(result);
            } else if (item.isCancelled()) {
                future.cancel();
            } else if (item.getCause() != null) {
                future.setFailure(item.getCause());
            }
        }
    }

    private static class TearDownConnectionIfFailureObserver implements Observer<ObservableFuture<SubscriptionCompleteCommand>> {

        final String clientId;
        final SignalProvider signalProvider;

        private TearDownConnectionIfFailureObserver(String clientId, SignalProvider signalProvider) {
            this.clientId = clientId;
            this.signalProvider = signalProvider;
        }

        @Override
        public void notify(Object sender, ObservableFuture<SubscriptionCompleteCommand> signalsConnectFuture) {

            SignalsConnectTask task = (SignalsConnectTask) sender;
            ConnectionHandle connectionHandle = task.getConnectionHandle();

            synchronized (signalProvider) {
                String currentClientId = signalProvider.getClientId();
                if (!StringUtil.equals(clientId, currentClientId)) {
                    LOGGER.warn(String.format("The clientId's changed. Not running tearDown. %s->%s", clientId, currentClientId));
                    return;
                }

                if (signalsConnectFuture.getCause() instanceof TimeoutException) {
                    LOGGER.error("Timeout on receiving the SubscriptionCompleteCommand from the server. We're going to tear down the connection and let the ReconnectStrategy take it from there. (If you dont see a disconnect it was because it already reconnected)");

                    // we are in the Timer thread (pub sub if Timer is Intent based).
                    // hashwheel otherwise.

                    // we've decided to clear the clientId when the signals/connect doesn't work
                    // this connection object lets us be certain that the current connection is reconnected.
                    signalProvider.resetDisconnectAndConnect();
                } else if (signalsConnectFuture.isCancelled()) {
                    // potentially we would be in the CALLER thread.
                    LOGGER.warn("Cancelled our /signals/connect web call future?!?");
                } else if (signalsConnectFuture.getCause() != null) {
                    LOGGER.error("Problem executing /signals/connect", signalsConnectFuture.getCause());

                    connectionHandle.disconnect(true);
                } else {
                    // guess it succeeded, do we care?
                    LOGGER.debug("Successfully got a SubscriptionCompleteCommand from the server! " + signalsConnectFuture.getResult());
                }
            }
        }

        @Override
        public String toString() {
            return "TearDownConnectionIfFailureObserver";
        }
    }

    public void setSignalProvider(SignalProvider signalProvider) {
        this.signalProvider = signalProvider;
    }

    public SettingsStore getSettingsStore() {
        return settingsStore;
    }

    public void setSettingsStore(SettingsStore store) {
        this.settingsStore = store;
        this.versionsStore = new SettingsVersionStore(store);
    }
}
