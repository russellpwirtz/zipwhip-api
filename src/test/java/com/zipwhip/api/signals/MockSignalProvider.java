package com.zipwhip.api.signals;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionHandleBase;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.api.signals.sockets.ConnectionStateManagerFactory;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.FakeObservableFuture;
import com.zipwhip.concurrent.NestedObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.NamedThreadFactory;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.StateManager;

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
    public final ObservableHelper<SubscriptionCompleteCommand> subscriptionCompleteEvent = new ObservableHelper<SubscriptionCompleteCommand>();
    private final ObservableHelper<Command> commandReceivedEvent = new ObservableHelper<Command>();

    protected StateManager<ConnectionState> stateManager;
    protected ObservableFuture<ConnectionHandle> disconnectingFuture = null;
    protected ObservableFuture<ConnectionHandle> connectingFuture;

    //    protected Executor executor = SimpleExecutor.getInstance();
    public Executor executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("MockSignalProvider-"));
    private ConnectionHandle connectionHandle;

    public MockSignalProvider() {
        stateManager = ConnectionStateManagerFactory.getInstance().create();
    }

    @Override
    public ConnectionState getConnectionState() {
        return stateManager.get();
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
        subscriptionCompleteEvent.notifyObservers(connectionHandle, command);
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
    public ConnectionHandle getConnectionHandle() {
        return connectionHandle;
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect() {
        return connect(null);
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect(String c)  {
        return connect(c, null);
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect(String c, Map<String, Long> versions) {
        return connect(c, versions, null);
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> connect(String c, Map<String, Long> versions, Presence presence) {

        if (connectingFuture != null) {
            return connectingFuture;
        } else if (disconnectingFuture != null){
            disconnectingFuture.cancel();
        }

        if (stateManager.get() == ConnectionState.CONNECTED) {
            return new FakeObservableFuture<ConnectionHandle>(null, null);
        }

        stateManager.transitionOrThrow(ConnectionState.CONNECTING);

        final ObservableFuture<ConnectionHandle> future = new DefaultObservableFuture<ConnectionHandle>(this);

        connectingFuture = future;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                ConnectionHandle connectionHandle = new MockSignalProviderConnectionHandle();
                synchronized (MockSignalProvider.this) {
                    if (connectingFuture.isCancelled()) {
                        return;
                    }

                    stateManager.ensure(ConnectionState.CONNECTING);

                    clientId = "1234567890";
                    isConnected = true;
                    stateManager.transitionOrThrow(ConnectionState.CONNECTED);
                    stateManager.transitionOrThrow(ConnectionState.AUTHENTICATED);

                    connectingFuture = null;

                    connectionChangedEvent.notify(connectionHandle, Boolean.TRUE);
                    newClientIdEvent.notify(connectionHandle, clientId);

                    MockSignalProvider.this.connectionHandle = connectionHandle;
                    future.setSuccess(connectionHandle);
                }
            }
        });

        return future;
    }

    @Override
    public ObservableFuture<ConnectionHandle> disconnect() {
        return disconnect(false);
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect(boolean causedByNetwork) {

        if (disconnectingFuture != null) {
            return disconnectingFuture;
        } else if (connectingFuture != null){
            connectingFuture.cancel();
        }

        if (stateManager.get() == ConnectionState.DISCONNECTED) {
            return new FakeObservableFuture<ConnectionHandle>(this, null);
        }

        final ObservableFuture<ConnectionHandle> result = connectionHandle.getDisconnectFuture();

        disconnectingFuture = result;
        stateManager.transitionOrThrow(ConnectionState.DISCONNECTING);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (MockSignalProvider.this) {
                    if (disconnectingFuture.isCancelled()) {
                        return;
                    }

                    stateManager.ensure(ConnectionState.DISCONNECTING);

                    isConnected = false;
                    stateManager.transitionOrThrow(ConnectionState.DISCONNECTED);

                    disconnectingFuture = null;

                    connectionChangedEvent.notify(this, Boolean.FALSE);

                    result.setSuccess(null);
                }
            }
        });

        return result;
    }

    @Override
    public ObservableFuture<ConnectionHandle> resetDisconnectAndConnect() {
        clientId = null;

        final NestedObservableFuture<ConnectionHandle> future = new NestedObservableFuture<ConnectionHandle>(this);


        disconnect().addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                future.setNestedFuture(connect());
            }
        });

        return future;
    }

    @Override
    public ObservableFuture<Boolean> ping() {
        //To change body of implemented methods use File | Settings | File Templates.
        return new FakeObservableFuture<Boolean>(this, true);
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
    public void destroy() {
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    static long id;

    private class MockSignalProviderConnectionHandle extends ConnectionHandleBase {

        protected MockSignalProviderConnectionHandle() {
            super(id++);
        }

        @Override
        protected void proxyDisconnectFromRequestorToParent(ObservableFuture<ConnectionHandle> disconnectFuture, boolean causedByNetwork) {
            MockSignalProvider.this.disconnect(causedByNetwork);
        }

        @Override
        public ObservableFuture<ConnectionHandle> reconnect() {
            final NestedObservableFuture<ConnectionHandle> future = new NestedObservableFuture<ConnectionHandle>(this);
            disconnect().addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
                @Override
                public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                    future.setNestedFuture(connect());
                }
            });
            return future;
        }

        @Override
        protected void onDestroy() {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}