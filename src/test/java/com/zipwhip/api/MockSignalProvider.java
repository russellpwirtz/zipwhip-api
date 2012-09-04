package com.zipwhip.api;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.api.signals.sockets.SignalProviderState;
import com.zipwhip.api.signals.sockets.SignalProviderStateManagerFactory;
import com.zipwhip.api.signals.sockets.StateManager;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.StringUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MockSignalProvider implements SignalProvider {

    boolean isConnected = false;
    String clientId;

    private final ObservableHelper<PingEvent> pingEvent = new ObservableHelper<PingEvent>();
    private final ObservableHelper<Boolean> connectionChangedEvent = new ObservableHelper<Boolean>();
    private final ObservableHelper<String> newClientIdEvent = new ObservableHelper<String>();
    private final ObservableHelper<List<Signal>> signalEvent = new ObservableHelper<List<Signal>>();
    private final ObservableHelper<List<SignalCommand>> signalCommandEvent = new ObservableHelper<List<SignalCommand>>();
    private final ObservableHelper<String> exceptionEvent = new ObservableHelper<String>();
    private final ObservableHelper<Void> signalVerificationEvent = new ObservableHelper<Void>();
    private final ObservableHelper<VersionMapEntry> newVersionEvent = new ObservableHelper<VersionMapEntry>();
    private final ObservableHelper<Boolean> presenceReceivedEvent = new ObservableHelper<Boolean>();
    private final ObservableHelper<SubscriptionCompleteCommand> subscriptionCompleteEvent = new ObservableHelper<SubscriptionCompleteCommand>();
    private final ObservableHelper<Command> commandReceivedEvent = new ObservableHelper<Command>();

    protected StateManager<SignalProviderState> stateManager;
    protected ObservableFuture<Void> disconnectingFuture = null;
    protected ObservableFuture<Boolean> connectingFuture;

    public MockSignalProvider() {
        try {
            stateManager = SignalProviderStateManagerFactory.getInstance().create();
        } catch (Exception e) {
            // bad api :(
        }
    }

//    protected Executor executor = SimpleExecutor.getInstance();
protected Executor executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("MockSignalProvider-"));

    public boolean isConnected() {
        if (stateManager.get() == SignalProviderState.AUTHENTICATED) {
            Asserts.assertTrue(isConnected, "The connection and stateManager disagreed! (Authenticated !connected)");
            return true;
        } else {
            if (stateManager.get() == SignalProviderState.CONNECTED) {
                Asserts.assertTrue(isConnected, "The connection and stateManager disagreed! (Connected !connected)");
            }

            return false;
        }
    }

    public boolean isAuthenticated() {
        return stateManager.get() == SignalProviderState.AUTHENTICATED;
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
        return null;
    }

    public void sendSubscriptionCompleteCommand(SubscriptionCompleteCommand command) {
        subscriptionCompleteEvent.notifyObservers(this, command);
    }

    @Override
    public void setPresence(Presence presence) {
    }

    @Override
    public Map<String, Long> getVersions() {
        return null;
    }

    @Override
    public void setVersions(Map<String, Long> versions) {
    }

    @Override
    public ObservableFuture<Boolean> connect() throws Exception {
        return connect(null);
    }

    @Override
    public ObservableFuture<Boolean> connect(String c) throws Exception {
        return connect(c, null);
    }

    @Override
    public ObservableFuture<Boolean> connect(String c, Map<String, Long> versions) throws Exception {
        return connect(c, versions, null);
    }

    @Override
    public synchronized ObservableFuture<Boolean> connect(String c, Map<String, Long> versions, Presence presence) throws Exception {

        if (connectingFuture != null) {
            return connectingFuture;
        }

        stateManager.transitionOrThrow(SignalProviderState.CONNECTING);

        final ObservableFuture<Boolean> future = new DefaultObservableFuture<Boolean>(this);

        connectingFuture = future;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (MockSignalProvider.this) {
                    clientId = "1234567890";
                    isConnected = true;
                    stateManager.transitionOrThrow(SignalProviderState.CONNECTED);
                    stateManager.transitionOrThrow(SignalProviderState.AUTHENTICATED);
                    connectionChangedEvent.notify(this, Boolean.TRUE);
                    newClientIdEvent.notify(this, clientId);

                    connectingFuture = null;
                    future.setSuccess(true);
                }
            }
        });

        return future;
    }

    @Override
    public ObservableFuture<Void> disconnect() throws Exception {
        return disconnect(false);
    }

    @Override
    public void resetAndDisconnect() throws Exception {
        disconnect();
    }

    @Override
    public synchronized ObservableFuture<Void> disconnect(boolean causedByNetwork) throws Exception {
        if (disconnectingFuture != null) {
            return disconnectingFuture;
        }

        final ObservableFuture<Void> result = new DefaultObservableFuture<Void>(this);

        if (connectingFuture != null){
            connectingFuture.cancel();
            connectingFuture = null;
        }

        disconnectingFuture = result;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                stateManager.set(SignalProviderState.DISCONNECTING);
                isConnected = false;
                stateManager.transitionOrThrow(SignalProviderState.DISCONNECTED);
                connectionChangedEvent.notify(this, Boolean.FALSE);
                disconnectingFuture = null;

                result.setSuccess(null);
            }
        });

        return result;
    }

    @Override
    public void nudge() {
    }

    @Override
    public Observable<List<Signal>> getSignalReceivedEvent() {
        return signalEvent;
    }

    @Override
    public Observable<List<SignalCommand>> getSignalCommandReceivedEvent() {
        return signalCommandEvent;
    }

    @Override
    public Observable<Boolean> getConnectionChangedEvent() {
        return connectionChangedEvent;
    }

    @Override
    public Observable<String> getNewClientIdReceivedEvent() {
        return newClientIdEvent;
    }

    @Override
    public Observable<SubscriptionCompleteCommand> getSubscriptionCompleteReceivedEvent() {
        return subscriptionCompleteEvent;
    }

    @Override
    public Observable<Boolean> getPhonePresenceReceivedEvent() {
        return presenceReceivedEvent;
    }

    @Override
    public Observable<Void> getSignalVerificationReceivedEvent() {
        return signalVerificationEvent;
    }

    @Override
    public Observable<VersionMapEntry> getVersionChangedEvent() {
        return newVersionEvent;
    }

    @Override
    public Observable<PingEvent> getPingReceivedEvent() {
        return pingEvent;
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
    public ObservableFuture<Void> runIfActive(final Runnable runnable) {
        final String clientId = this.clientId;
        final boolean connected = isConnected();
        final ObservableFuture<Void> future = new DefaultObservableFuture<Void>(this, executor);

        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // dont let the clientId be changed while we compare.
                    synchronized (MockSignalProvider.this) {
                        boolean c = isConnected();
                        if (connected != c) {
                            future.setFailure(new Exception());
                            //                                LOGGER.warn("Not currently connected, so not going to execute this runnable " + runnable);
                            return;
                        } else if (!StringUtil.equals(MockSignalProvider.this.getClientId(), clientId)) {
                            //                                LOGGER.warn("We avoided a race condition by detecting the clientId changed. Not going to run this runnable " + runnable);
                            future.setFailure(new Exception());
                            return;
                        }

                        try {
                            runnable.run();
                        } finally {
                            future.setSuccess(null);
                        }
                    }
                }
            });
        } catch (RuntimeException e) {
            future.setFailure(e);
        }

        return future;
    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

}