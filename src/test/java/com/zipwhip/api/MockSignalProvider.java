package com.zipwhip.api;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.FakeObservableFuture;
import com.zipwhip.signals.presence.Presence;
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

    private Executor executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("MockSignalProvider-"));

    @Override
    public boolean isConnected() {
        return isConnected;
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
        this.clientId = "1234567890";
        isConnected = true;
        connectionChangedEvent.notify(this, Boolean.TRUE);
        newClientIdEvent.notify(this, clientId);
        return new FakeObservableFuture<Boolean>(this, Boolean.TRUE);
    }

    @Override
    public ObservableFuture<Boolean> connect(String c) throws Exception {
        this.clientId = "1234567890";
        isConnected = true;
        connectionChangedEvent.notify(this, Boolean.TRUE);
        newClientIdEvent.notify(this, this.clientId);
        return new FakeObservableFuture<Boolean>(this, Boolean.TRUE);
    }

    @Override
    public ObservableFuture<Boolean> connect(String c, Map<String, Long> versions) throws Exception {
        this.clientId = "1234567890";
        isConnected = true;
        connectionChangedEvent.notify(this, Boolean.TRUE);
        newClientIdEvent.notify(this, this.clientId);
        return new FakeObservableFuture<Boolean>(this, Boolean.TRUE);
    }

    @Override
    public ObservableFuture<Boolean> connect(String c, Map<String, Long> versions, Presence presence) throws Exception {
        this.clientId = "1234567890";
        isConnected = true;
        connectionChangedEvent.notify(this, Boolean.TRUE);
        newClientIdEvent.notify(this, this.clientId);
        return new FakeObservableFuture<Boolean>(this, Boolean.TRUE);
    }

    @Override
    public ObservableFuture<Void> disconnect() throws Exception {
        isConnected = false;
        connectionChangedEvent.notifyObservers(this, Boolean.FALSE);
        return new FakeObservableFuture<Void>(this, null);
    }

    @Override
    public void resetAndDisconnect() throws Exception {
        disconnect();
    }

    @Override
    public ObservableFuture<Void> disconnect(boolean causedByNetwork) throws Exception {
        isConnected = false;
        connectionChangedEvent.notify(this, Boolean.FALSE);
        return new FakeObservableFuture<Void>(this, null);
    }

    @Override
    public void nudge() {
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
        connectionChangedEvent.addObserver(observer);
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
    public void runIfActive(final Runnable runnable) {
        final String clientId = this.clientId;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                // dont let the clientId be changed while we compare.
                synchronized (MockSignalProvider.this) {
                    if (!isConnected()) {
//                                LOGGER.warn("Not currently connected, so not going to execute this runnable " + runnable);
                        return;
                    } else if (!StringUtil.equals(MockSignalProvider.this.getClientId(), clientId)) {
//                                LOGGER.warn("We avoided a race condition by detecting the clientId changed. Not going to run this runnable " + runnable);
                        return;
                    }

                    runnable.run();
                }
            }
        });
    }

    @Override
    public void removeOnSubscriptionCompleteObserver(Observer<SubscriptionCompleteCommand> observer) {
        subscriptionCompleteEvent.removeObserver(observer);
    }

    @Override
    public void removeOnConnectionChangedObserver(Observer<Boolean> observer) {
        connectionChangedEvent.removeObserver(observer);
    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

}