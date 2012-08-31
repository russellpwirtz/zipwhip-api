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
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.FakeFailingObservableFuture;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
    protected Executor executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("ZipwhipClient-"));

    protected SignalProvider signalProvider;
    protected SettingsStore settingsStore = new PreferencesSettingsStore();
    protected VersionStore versionsStore = new SettingsVersionStore(settingsStore);

    protected ObservableFuture<Boolean> connectingFuture;
    protected ObservableFuture<Void> disconnectFuture;

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
        signalProvider.onConnectionChanged(failConnectingFutureIfDisconnectedObserver);
//        signalProvider.onSubscriptionComplete(releaseLatchOnSubscriptionComplete);
        signalProvider.onNewClientIdReceived(executeSignalsConnectOnNewClientIdReceived);
        signalProvider.onVersionChanged(updateVersionsStoreOnVersionChanged);
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

    public ObservableFuture<Boolean> connect() throws Exception {
        return connect(null);
    }

    public synchronized ObservableFuture<Boolean> connect(final Presence presence) throws Exception {

        // if we are already connecting, don't do another connect.
        if (connectingFuture != null) {
            return connectingFuture;
        }

        // we need to determine if we're authenticated enough
        if (!connection.isConnected() || !connection.isAuthenticated()) {
            throw new NotAuthenticatedException("The connection cannot operate at this time");
        }

        if (getSignalProvider().isConnected()) {
            // we are already connected
            throw new Exception("Already connected");
        }

        // create the resulting future. We need a "final" reference to this in order to
        // guarantee correct thread access. If someone changes our stuff, don't screw them up.
        final ObservableFuture<Boolean> finalConnectingFuture = connectingFuture = createConnectingFuture();
        try {
            // only run if-only-if the clientId/sessionKey state hasn't changed by the time we run.
            runIfActive(new Runnable() {

                @Override
                public void run() {
                    // synchronize on the future.
                    // Our sync cycle has been Client -> Connection -> SignalProvider -> Future
                    synchronized (finalConnectingFuture) {
                        try {
                            Asserts.assertTrue(finalConnectingFuture == connectingFuture, "Some mysterious threading mistake happened. ConnectingFuture changed from underneath us.");
                            Map<String, Long> versions = versionsStore.get();

                            executeConnect(finalConnectingFuture, versions, presence);
                        } catch (Exception e) {
                            finalConnectingFuture.setFailure(e);
                        }
                    }
                }
            });
        } catch (Exception e) {
            // do this so other callers aren't blocked from piercing into the connect() method
            connectingFuture = null;
            throw e;
        }

        return finalConnectingFuture;
    }

    private void executeConnect(ObservableFuture<Boolean> connectingFuture, Map<String, Long> versions, Presence presence) {
        boolean expectingSubscriptionCompleteCommand = false;

        /**
         * Validate SESSION KEYS
         */
        String existingSessionKey = connection.getSessionKey();
        String storedSessionKey = settingsStore.get(SettingsStore.Keys.SESSION_KEY);
        String correctSessionKey = storedSessionKey;

        // If the sessionKey has changed we need to invalidate the settings data
        if (StringUtil.exists(existingSessionKey) && !StringUtil.equals(existingSessionKey, storedSessionKey)) {
            expectingSubscriptionCompleteCommand = true;
            LOGGER.debug("New or changed sessionKey, resetting session key in settings store");
            settingsStore.clear();

            correctSessionKey = connection.getSessionKey();

            settingsStore.put(SettingsStore.Keys.SESSION_KEY, correctSessionKey);
        }

        /**
         * Validate CLIENT IDs
         */
        String existingClientId = signalProvider == null ? null : signalProvider.getClientId();
        String storedClientId = settingsStore.get(SettingsStore.Keys.CLIENT_ID);
        String correctClientId = existingClientId;

        /**
         * If the clientId has changed we need to invalidate the settings data
         */
        if (StringUtil.isNullOrEmpty(storedClientId) || (StringUtil.exists(existingClientId) && !StringUtil.equals(storedClientId, existingClientId))) {
            expectingSubscriptionCompleteCommand = true;
            LOGGER.debug("ClientId has changed, resetting client id in settings store");

            settingsStore.clear();

            correctClientId = existingClientId;

            if (existingClientId != null) {
                settingsStore.put(SettingsStore.Keys.CLIENT_ID, existingClientId);
            }
            // put back the 'correct session key' since we just cleared it
            settingsStore.put(SettingsStore.Keys.SESSION_KEY, correctSessionKey);
        }

        /**
         * This is the way we block the connectingFuture conditionally.
         */
        final ObservableFuture<Boolean> finalConnectingFuture = connectingFuture;
        final ObservableFuture<Boolean> requestFuture;
        try {
            requestFuture = signalProvider.connect(correctClientId, versions, presence);
        } catch (Exception e) {
            connectingFuture.setFailure(e);
            return;
        }

        LOGGER.debug("Expecting subscriptionComplete: " + expectingSubscriptionCompleteCommand);
        if (expectingSubscriptionCompleteCommand) {
            // only run the "reset connectingFuture" observable if this initial internet request fails (timeout, socket exception, etc).
            requestFuture.addObserver(new OnlyRunIfFailedObserverAdapter<Boolean>(new CopyFutureStatusToNestedFuture(finalConnectingFuture)));
            // if this requestFuture succeeds, we need to let the onSubscriptionCompleteCommand finish the "connectingFuture"
        } else {
            requestFuture.addObserver(new CopyFutureStatusToNestedFuture(finalConnectingFuture));
        }
    }

    public synchronized ObservableFuture<Void> disconnect() throws Exception {
        return disconnect(false);
    }

    public synchronized ObservableFuture<Void> disconnect(final boolean causedByNetwork) throws Exception {
        if (!connection.isConnected()) {
            return new FakeFailingObservableFuture<Void>(this, new Exception("Not currently connected, no session?"));
        } else if (!signalProvider.isConnected()) {
            return new FakeFailingObservableFuture<Void>(this, new Exception("Not currently connected, internet down? no clientId?"));
        }

        if (disconnectFuture != null) {
            return disconnectFuture;
        }

        final NestedObservableFuture<Void> result = new NestedObservableFuture<Void>(this, executor);
        disconnectFuture = result;

        // if the future finishes, clean up the global reference.
        result.addObserver(new ResetDisconnectFutureObserver(this));

        /**
         * This method will run later on the core executor thread. The key being that you can only disconnect/connect
         * once at the same time.
         */
        runSafely(new Runnable() {

            @Override
            public void run() {
                // while in this method, the disconnectFuture cannot change ownership.
                // this check will guarantee accuracy for the duration of "run"
                Asserts.assertTrue(disconnectFuture == result, "Assumption on safety failed.");

                ObservableFuture<Void> requestFuture;

                try {
                    requestFuture = signalProvider.disconnect(causedByNetwork);
                } catch (Exception e) {
                    result.setFailure(e);
                    disconnectFuture = null;
                    return;
                }

                // the nesting will help coordinate the two futures. (parent/child)
                requestFuture.addObserver(new Observer<ObservableFuture<Void>>() {
                    @Override
                    public void notify(Object sender, ObservableFuture<Void> item) {
                        // this is the Connection thread
                        LOGGER.error("Request future called.");
                    }
                });

                // the nesting will help coordinate the two futures. (parent/child)
                result.addObserver(new Observer<ObservableFuture<Void>>() {
                    @Override
                    public void notify(Object sender, ObservableFuture<Void> item) {
                        // this is the executor thread.
                        LOGGER.error("Request future called.");
                    }
                });

                result.setNestedFuture(requestFuture);
            }
        });

        return result;
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

    private final Observer<Boolean> failConnectingFutureIfDisconnectedObserver = new Observer<Boolean>() {

        @Override
        public void notify(Object sender, Boolean connected) {
            boolean conn = connected.booleanValue();

            if (!conn) {
                /**
                 * We are in the SignalProvider thread. It's against the law to synchronize the Client in the wrong order.
                 * We require that the order is Client -> Provider. Since you can't guarantee what thread you are in or
                 * what lock order you have, we will just do a runIfActive.
                 */
                runIfActive(new Runnable() {
                    @Override
                    public void run() {
                        if (connectingFuture != null) {
                            connectingFuture.setFailure(new Exception("Disconnected"));
                        }
                    }
                });
            }
        }
    };

    private final Observer<VersionMapEntry> updateVersionsStoreOnVersionChanged = new Observer<VersionMapEntry>() {

        @Override
        public void notify(Object sender, VersionMapEntry item) {
            versionsStore.set(item.getKey(), item.getValue());
        }
    };

    private final Observer<String> executeSignalsConnectOnNewClientIdReceived = new Observer<String>() {

        @Override
        public void notify(Object sender, String newClientId) {

            // NOTE: we need to allow this in order to do a clean reset
            //                if (StringUtil.isNullOrEmpty(newClientId)) {
            //                    LOGGER.warn("Received CONNECT without clientId");
            //                    return;
            //                }

            // NOTE: when we do a reset, we're going to get a new clientId that is null
            if (StringUtil.isNullOrEmpty(newClientId)) {
                settingsStore.put(SettingsStore.Keys.CLIENT_ID, newClientId);
                return;
            }

            String oldClientId = settingsStore.get(SettingsStore.Keys.CLIENT_ID);
            final String sessionKey = connection.getSessionKey();

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
                    executeSyncSucceedOrDisconnect(SIGNALS_DISCONNECT, params);

                    executeSignalsConnect(newClientId, sessionKey);
                }
            } else {
                settingsStore.put(SettingsStore.Keys.CLIENT_ID, newClientId);

                // lets do a signals connect!
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("clientId", newClientId);
                params.put("sessions", sessionKey);

                Presence presence = signalProvider.getPresence();

                if (presence != null) {
                    params.put("category", presence.getCategory());
                }

                executeSignalsConnect(newClientId, sessionKey);
            }
        }
    };

    /**
     * Execute a /signals/connect webcall s
     *
     * @param clientId
     * @param sessionKey
     * @return
     */
    private synchronized ObservableFuture<SubscriptionCompleteCommand> executeSignalsConnect(final String clientId, final String sessionKey) {

        /**
         * This is a call that will time out. We had to do the "importantTaskExecutor" in order to allow Android AlarmManager
         * to run the scheduling.
         */
        final ObservableFuture<SubscriptionCompleteCommand> future =
                importantTaskExecutor.enqueue(executor, new SignalsConnectTask(sessionKey, clientId), signalsConnectTimeoutInSeconds);

//        final String sessionKey = connection.getSessionKey();

        future.addObserver(new Observer<ObservableFuture<SubscriptionCompleteCommand>>() {

            /**
             * This method will fire when the future completes either by failure/success/cancellation. Technically
             * the cancel won't stop the process from processing, however it will terminate/teardown this future and
             * if the process finishes later (or times out again) we're already destroyed and wont be called again.
             * This method will be called once and only once no matter how many times you call .cancel() or .setSuccess();
             *
             * The thread of this notify method is the future.executor
             *          (which is either HashWheelTimer, Intent/pubsub, OR the
             *
             * @param sender
             * @param item
             */
            @Override
            public void notify(Object sender, ObservableFuture<SubscriptionCompleteCommand> item) {

                synchronized (ClientZipwhipNetworkSupport.this) {
                    if (connectingFuture != null) {
                        NestedObservableFuture.syncStateBoolean(item, connectingFuture);
                    }
                }

                if (!future.isSuccess()) {
                    if (future.getCause() instanceof TimeoutException) {
                        LOGGER.error("Timeout on receiving the SubscriptionCompleteCommand from the server. We're going to tear down the connection and let the ReconnectStrategy take it from there. (If you dont see a disconnect it was because it already reconnected)");

                        // we are in the Timer thread (pub sub if Timer is Intent based).
                        // hashwheel otherwise.
                        // TODO: we need to be 100% certain that this signalProvider is the SAME exact connection that we started with.

                        // we've decided to clear the clientId when the signals/connect doesn't work

                        signalProvider.runIfActive(new Runnable() {

                            @Override
                            public void run() {
                                LOGGER.debug("signalProvider.runIfActive() hit. We're going to tear down this connection. We can trust that it wont change during this method.");

                                // This thread is the connection thread (deadlock if block on connect / disconnect)

                                // we need to synchronize on the CONNECTION in order to guarantee that no one can change
                                // the sessionKey while we're working on it.
                                synchronized (connection) {
                                    String s = connection.getSessionKey();
                                    String c = signalProvider.getClientId();
                                    if (!StringUtil.equals(sessionKey, s)) {
                                        LOGGER.warn(String.format("The sessionKey changed from underneath us. [%s->%s] Just quitting", sessionKey, s));
                                        return;
                                    } else if (!StringUtil.equals(clientId, c)) {
                                        LOGGER.warn(String.format("The clientId changed from underneath us. [%s->%s] Just quitting", clientId, c));
                                        return;
                                    }

                                    try {
                                        LOGGER.debug("We're safely in the same connection as we were before, so we're going to tear down the connection since we missed a SubscriptionCompleteCommand");
                                        signalProvider.resetAndDisconnect();
                                    } catch (Exception e) {
                                        LOGGER.error("");
                                    }
                                }
                            }
                        });
                    } else if (future.isCancelled()) {
                        // potentially we would be in the CALLER thread.
                        LOGGER.warn("Cancelled our /signals/connect web call future?!?");
                    } else if (future.getCause() != null) {
                        LOGGER.error("Problem executing /signals/connect", future.getCause());
                        if (signalProvider.isConnected()) {
                            try {
                                signalProvider.disconnect(true);
                            } catch (Exception e) {
                                // TODO: do we retry or are we an orphaned whale now?
                                LOGGER.error("Even tried to execute provider.disconnect() and got an error!", e);
                            }
                        }
                    } else {
                        // guess it succeeded, do we care?
                        LOGGER.debug("Successfully got a SubscriptionCompleteCommand from the server! " + item.getResult());
                    }
                }
            }
        });

//        future.addObserver(new Observer<ObservableFuture<SubscriptionCompleteCommand>>() {
//            @Override
//            public void notify(Object sender, ObservableFuture<SubscriptionCompleteCommand> item) {
//                latch.countDown();
//            }
//        });

        return future;
    }


    private ObservableFuture<Boolean> createConnectingFuture() {
        final ObservableFuture<Boolean> result = new DefaultObservableFuture<Boolean>(this, executor);

        result.addObserver(new Observer<ObservableFuture<Boolean>>() {

            /**
             * This thread is the "executor" thread because of the constructor for the ObservableFuture
             *
             * @param sender
             * @param item
             */
            @Override
            public void notify(Object sender, ObservableFuture<Boolean> item) {
                synchronized (ClientZipwhipNetworkSupport.this) {
                    Asserts.assertTrue(result == connectingFuture, "Odd that the two futures were not identical. Race condition?");

                    connectingFuture = null;
                }
            }
        });

        return result;
    }

    private void runSafely(final Runnable runnable) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (ClientZipwhipNetworkSupport.this) {
                    runnable.run();
                }
            }
        });
    }

    protected void runIfActive(final Runnable runnable) {
        final SignalProvider provider = signalProvider;
        final Connection conn = connection;
        final String clientId = provider.getClientId();
        final String sessionKey = conn.getSessionKey();

        LOGGER.debug("runIfActive called for runnable: " + runnable);
        // NOTE: due to deadlocks we cannot "synchronized" in this method on the ZipwhipClient.
        // we will do a bunch of synchronizing in the runnable.
        runSafely(new Runnable() {
            @Override
            public void run() {
                synchronized (provider) {
                    synchronized (conn) {
                        if (!StringUtil.equals(sessionKey, conn.getSessionKey())) {
                            LOGGER.warn("The sessionKey changed while we were waiting to run. Not running runnable: " + runnable);
                            return;
                        } else if (!StringUtil.equals(clientId, provider.getClientId())) {
                            LOGGER.warn(String.format("The clientId changed while we were waiting to run. [%s->%s]. Not running runnable: %s", clientId, provider.getClientId(), runnable));
                            return;
                        }

                        // you are now safe to run padiwan.
                        LOGGER.debug("(running) runIfActive called for runnable: " + runnable);
                        runnable.run();
                    }
                }
            }
        });
    }

    private class SignalsConnectTask implements Callable<ObservableFuture<SubscriptionCompleteCommand>> {

        private final String sessionKey;
        private final String clientId;

        private SignalsConnectTask(String sessionKey, String clientId) {
            this.sessionKey = sessionKey;
            this.clientId = clientId;
        }

        @Override
        public ObservableFuture<SubscriptionCompleteCommand> call() throws Exception {
            Map<String, Object> params = new HashMap<String, Object>();

            params.put("sessions", sessionKey);
            params.put("clientId", clientId);

            final ObservableFuture<SubscriptionCompleteCommand> resultFuture = new DefaultObservableFuture<SubscriptionCompleteCommand>(this);

            final Observer<Boolean>[] onDisconnectObserver = new Observer[1];

            final Observer<SubscriptionCompleteCommand> onSubscriptionCompleteObserver = new Observer<SubscriptionCompleteCommand>() {
                @Override
                public void notify(Object sender, SubscriptionCompleteCommand item) {
                    signalProvider.removeOnSubscriptionCompleteObserver(this);
                    signalProvider.removeOnConnectionChangedObserver(onDisconnectObserver[0]);

                    LOGGER.debug("Successing");
                    resultFuture.setSuccess(item);
                }
            };

            onDisconnectObserver[0] = new Observer<Boolean>() {
                @Override
                public void notify(Object sender, Boolean item) {
                    // on any kind of connection change, we need to just abort
                    signalProvider.removeOnSubscriptionCompleteObserver(onSubscriptionCompleteObserver);
                    signalProvider.removeOnConnectionChangedObserver(onDisconnectObserver[0]);

                    LOGGER.debug("Failing (disconected)");
                    resultFuture.setFailure(new Exception("Disconnected while waiting for SubscriptionCompleteCommand to come in! " + item));
                }
            };

            signalProvider.onConnectionChanged(onDisconnectObserver[0]);
            signalProvider.onSubscriptionComplete(onSubscriptionCompleteObserver);

            if (resultFuture.isDone()) {
                // wow it finished already?
                return resultFuture;
            }

            ServerResponse response = null;
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
                    signalProvider.removeOnConnectionChangedObserver(onDisconnectObserver[0]);
                    signalProvider.removeOnSubscriptionCompleteObserver(onSubscriptionCompleteObserver);
                }
            });

            LOGGER.debug("/signals/connect executed successfully. You should get back a SubscriptionCompleteCommand any time now. (Maybe already?)");

            return resultFuture;
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
                LOGGER.debug("Did not notify observer because not successful. " + item);
            }
        }
    }

    private static class ResetDisconnectFutureObserver implements Observer<ObservableFuture<Void>> {

        final ClientZipwhipNetworkSupport client;

        private ResetDisconnectFutureObserver(ClientZipwhipNetworkSupport client) {
            this.client = client;
        }

        @Override
        public void notify(Object sender, ObservableFuture<Void> item) {
            LOGGER.debug("Our disconnectFuture has finished, so we are going to reset it (only 'if active').");
            // this will only run if the state hasn't changed between enqueue and execute.
            // otherwise it will log/return.
            synchronized (client) {
                    LOGGER.debug("Resetting the disconnectFuture so that other people can call disconnect.");
                    client.disconnectFuture = null;
            }
        }
    }

    private static class ResetConnectingFutureObserver implements Observer<ObservableFuture<Void>> {

        final ClientZipwhipNetworkSupport client;

        private ResetConnectingFutureObserver(ClientZipwhipNetworkSupport client) {
            this.client = client;
        }

        @Override
        public void notify(Object sender, ObservableFuture<Void> item) {
            // this will only run if the state hasn't changed between enqueue and execute.
            // otherwise it will log/return.
            client.runIfActive(new Runnable() {
                @Override
                public void run() {
                    client.connectingFuture = null;
                }
            });
        }
    }


    protected void executeSyncSucceedOrDisconnect(String method, final Map<String, Object> params) {
        try {
            ServerResponse response = executeSync(method, params);

            if (response == null || !response.isSuccess()) {

                LOGGER.error("Error making a web call, try to disconnect...");

                try {
                    signalProvider.disconnect();
                } catch (Exception e) {
                    LOGGER.error("Failed to disconnect after web call failure...");
                }
            }
        } catch (Exception e) {
            try {
                signalProvider.disconnect();
            } catch (Exception ex) {
                LOGGER.error("Failed to disconnect after web call failure...");
            }
        }
    }

}
