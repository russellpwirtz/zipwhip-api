package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.executors.DebuggingExecutor;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.Factory;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.zipwhip.concurrent.ThreadUtil.ensureLock;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/8/12
 * Time: 3:28 PM
 */
public abstract class SignalProviderBase extends CascadingDestroyableBase implements SignalProvider {

    private static final Logger LOGGER = Logger.getLogger(SignalProviderBase.class);

    protected static Factory<Long> CONNECTION_HANDLE_ID_FACTORY = IncrementingLongFactory.getInstance();
    protected final Object CONNECTION_HANDLE_LOCK = new Object() {
        @Override
        public String toString() {
            return "CONNECTION_HANDLE_LOCK";
        }
    };

    protected final ObservableHelper<PingEvent> pingReceivedEvent = new ObservableHelper<PingEvent>("pingReceivedEvent");
    protected final ObservableHelper<List<Signal>> signalReceivedEvent = new ObservableHelper<List<Signal>>("signalReceivedEvent");
    protected final ObservableHelper<List<SignalCommand>> signalCommandReceivedEvent = new ObservableHelper<List<SignalCommand>>("signalCommandReceivedEvent");
    protected final ObservableHelper<Void> signalVerificationReceivedEvent = new ObservableHelper<Void>("signalVerificationReceivedEvent");
    protected final ObservableHelper<Command> commandReceivedEvent = new ObservableHelper<Command>("commandReceivedEvent");
    protected final ObservableHelper<Boolean> presenceReceivedEvent = new ObservableHelper<Boolean>("presenceReceivedEvent");
    protected final ObservableHelper<String> newClientIdReceivedEvent = new ObservableHelper<String>("newClientIdReceivedEvent");

    protected final ObservableHelper<Boolean> connectionChangedEvent = new ObservableHelper<Boolean>("connectionChangedEvent");
    protected final ObservableHelper<String> exceptionEvent = new ObservableHelper<String>("exceptionEvent");
    protected final ObservableHelper<VersionMapEntry> newVersionEvent = new ObservableHelper<VersionMapEntry>("newVersionEvent");
    protected final ObservableHelper<SubscriptionCompleteCommand> subscriptionCompleteReceivedEvent = new ObservableHelper<SubscriptionCompleteCommand>("subscriptionCompleteReceivedEvent");

    protected final Executor executor;

    private ObservableFuture<ConnectionHandle> connectFuture;
    private SignalProviderConnectionHandle connectionHandle;

    protected String clientId;
    protected String originalClientId;
    protected Presence presence;
    protected Map<String, Long> versions = new TreeMap<String, Long>();

    public SignalProviderBase(Executor executor) {

        if (executor == null) {
            executor = new DebuggingExecutor(Executors.newSingleThreadExecutor(new NamedThreadFactory("SignalProvider-events-")));
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ((ExecutorService) SignalProviderBase.this.executor).shutdownNow();
                }
            });
        }
        this.executor = executor;

        this.initEvents();
    }

    protected void initEvents() {
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
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect() {
        // NOTE: not sure this is correct. it was here in legacy code.
        return connect(originalClientId);
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect(String clientId) {
        return connect(clientId, versions);
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect(String clientId, Map<String, Long> versions) {
        return connect(clientId, versions, presence);
    }

    @Override
    public ObservableFuture<ConnectionHandle> disconnect() {
        return disconnect(false);
    }

    protected synchronized ObservableFuture<ConnectionHandle> disconnect(ConnectionHandle connectionHandle, boolean causedByNetwork) {
        if (connectionHandle == null) {
            throw new NullPointerException("Connection cannot be null");
        }

        synchronized (CONNECTION_HANDLE_LOCK) {
            synchronized (connectionHandle) {
                ConnectionHandle finalConnectionHandle = getUnchangingConnectionHandle();
                if (finalConnectionHandle == null || finalConnectionHandle.isDestroyed()) {
                    throw new IllegalStateException("The connection cannot be destroyed");
                } else if (connectionHandle.getDisconnectFuture().isDone()) {
                    return connectionHandle.getDisconnectFuture();
                }

                if (finalConnectionHandle == connectionHandle) {
                    return disconnect(causedByNetwork);
                } else {
                    throw new IllegalStateException("How can the future not be done, but not currently active?");
                }
            }
        }
    }

    protected void accessConnectionHandle() {
        ensureLock(SignalProviderBase.this);
        ensureLock(CONNECTION_HANDLE_LOCK);
    }

    /**
     * Ensures that you have access to change the currentConnectionHandle
     *
     * @param connectionHandle
     */
    protected void changeConnectionHandle(ConnectionHandle connectionHandle) {
        accessConnectionHandle();
        ConnectionHandle c = getUnchangingConnectionHandle();
        if (c != null) {
            Asserts.assertTrue(connectionHandle == c, String.format("Caught a bug with check? %s/%s", connectionHandle, c));
        }

        ensureLock(connectionHandle);
    }

    protected SignalProviderConnectionHandle getUnchangingConnectionHandle() {
        accessConnectionHandle();
        return connectionHandle;
    }

    protected void clearConnectionHandle(final ConnectionHandle finalConnectionHandle) {
        changeConnectionHandle(finalConnectionHandle);

        connectionHandle = null;
    }

    /**
     * Determines if you have access to change the current connectFuture (you have to know what it is first).
     *
     * @param future
     */
    protected void changeConnectFuture(ObservableFuture<ConnectionHandle> future) {
        accessConnectFuture();

        if (connectFuture != null) {
            Asserts.assertTrue(future == connectFuture, String.format("Caught a bug with check? %s/%s", future, connectFuture));
        }

        ensureLock(connectFuture);
        ensureLock(future);
    }

    protected void setConnectFuture(ObservableFuture<ConnectionHandle> future) {
        changeConnectFuture(future);

        if (connectFuture != null) {
            throw new IllegalStateException("The current connectFuture is not null. Must call clearConnectFuture() first.");
        }

        connectFuture = future;
    }

    protected void accessConnectFuture() {
        ensureLock(SignalProviderBase.this);
    }

    protected void clearConnectFuture(ObservableFuture<ConnectionHandle> future) {
        changeConnectFuture(future);
        // now we have exclusive ensureAbleTo to connectFuture. It can't change without our permission.

        if (connectFuture != null) {
            Asserts.assertTrue(future == connectFuture, String.format("Caught a bug with check? %s/%s", future, connectFuture));
        }

        connectFuture = null;
    }

    protected boolean ensureCorrectConnectionHandle(ConnectionHandle connectionHandle) {
        accessConnectionHandle();

        return (connectionHandle == this.getUnchangingConnectionHandle());
    }

    protected void setConnectionHandle(SignalProviderConnectionHandle connectionHandle) {
        if (connectionHandle == null) {
            throw new IllegalAccessError("Cannot set to null. Use 'clearConnectionHandle' instead");
        }

        accessConnectionHandle();

        if (getUnchangingConnectionHandle() != null) {
            throw new IllegalAccessError("The current connectionHandle is not null! Use clear first.");
        }

        this.connectionHandle = connectionHandle;
    }

    protected SignalProviderConnectionHandle newConnectionHandle() {
        return newConnectionHandle(null);
    }

    /**
     * @param connectionHandle The underlying connectionHandle (like a netty one).
     */
    protected SignalProviderConnectionHandle newConnectionHandle(ConnectionHandle connectionHandle) {
        return new SignalProviderConnectionHandle(CONNECTION_HANDLE_ID_FACTORY.create(), this, null);
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

    protected ObservableFuture<ConnectionHandle> getUnchangingConnectFuture() {
        accessConnectFuture();

        return getConnectFuture();
    }

    protected ObservableFuture<ConnectionHandle> getConnectFuture() {
        return connectFuture;
    }

    protected void cancelConnectFuture() {
        accessConnectFuture();
        changeConnectFuture(connectFuture);

        ObservableFuture<ConnectionHandle> f = connectFuture;
        clearConnectFuture(f);
        cancelFuture(f);
    }

    private void cancelFuture(ObservableFuture<?> future) {
        if (future == null) {
            return;
        }

        ensureLock(future);

        if (future.isDone()) {
            return;
        }
        future.cancel();
    }

    protected SignalProviderConnectionHandle getCurrentConnectionHandle() {
        return connectionHandle;
    }
}
