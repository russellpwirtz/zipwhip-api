package com.zipwhip.api.signals;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.api.signals.sockets.SignalProviderState;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.signals.presence.Presence;

import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/1/11 Time: 4:22 PM
 */
public interface SignalProvider extends Destroyable {

    /**
     * Determines if the SignalProvider is connected or not.
     *
     * @return true if the SignalProvider is connected else false.
     */
    SignalProviderState getState();

    /**
     * Every time the state changes, we will change the Version. This will help you keep track of
     * 'did the state change while i was waiting to execute'
     *
     * @return
     */
    long getStateVersion();


    /**
     * The SignalServer uses a separate id to track you, because it's an Id
     * given to a TCP/IP connection, not a user.
     *
     * @return the id that the SignalServer has for us
     */
    String getClientId();

    /**
     * Get the current Presence object or null
     *
     * @return The current Presence object or null
     */
    Presence getPresence();

    /**
     * Set the Presence to use on the next connection.
     *
     * @param presence The Presence to use on the next connection.
     */
    void setPresence(Presence presence);

    /**
     * Get the current versions or null
     *
     * @return The current versions or null
     */
    Map<String, Long> getVersions();

    /**
     * Set the signal versions to use on the next connection.
     *
     * @param versions The signal versions to use on the next connection.
     */
    void setVersions(Map<String, Long> versions);

    /**
     * Tell it to connect. This call is idempotent, so if multiple calls to
     * a connection provider will have no effect.
     *
     * @return a ObservableFuture task indicating if the connection was successful.
     * @throws Exception if an error is encountered when connecting
     */
    ObservableFuture<Boolean> connect() throws Exception;

    /**
     * Tell it to connect.
     *
     * @param clientId Pass in null if you don't have one.
     * @return A future that tells you when the connecting is complete. The
     *         string result is the clientId.
     * @throws Exception if an I/O happens while connecting
     */
    ObservableFuture<Boolean> connect(String clientId) throws Exception;

    /**
     * Tell it to connect.
     *
     * @param clientId Pass in null if you don't have one.
     * @param versions a Map of the current signal version per subscription.
     * @return A future that tells you when the connecting is complete. The
     *         string result is the clientId.
     * @throws Exception if an I/O happens while connecting.
     */
    ObservableFuture<Boolean> connect(String clientId, Map<String, Long> versions) throws Exception;

    /**
     * Tell it to connect. The future will unblock when {action:"CONNECT"} comes back
     *
     * @param clientId Pass in null if you don't have one.
     * @param versions a Map of the current signal version per subscription.
     * @param presence A Presence object to send on connect.
     * @return A future that tells you when the connecting is complete. The
     *         string result is the clientId.
     * @throws Exception if an I/O happens while connecting.
     */
    ObservableFuture<Boolean> connect(String clientId, Map<String, Long> versions, Presence presence) throws Exception;

    /**
     * Tell it to disconnect.
     *
     * @return an event that tells you its complete
     * @throws Exception if an I/O happens while disconnecting
     */
    ObservableFuture<Void> disconnect() throws Exception;

    /**
     * Tell it to disconnect. Call this with false is equivalent to calling {@code disconnect()}.
     * Calling it with true will cause the connection to go into reconnect mode once the disconnect happens.
     *
     * @param causedByNetwork If the network state caused the disconnect
     * @return an event that tells you its complete
     * @throws Exception if an I/O happens while disconnecting
     */
    ObservableFuture<Void> disconnect(boolean causedByNetwork) throws Exception;

    /**
     * Will reset the state, followed by a disconnect, followed by an immediate reconnect.
     *
     * @throws Exception
     */
    void resetAndDisconnect() throws Exception;

    /**
     * This little function is a BIG deal when you are running on a platform that freezes your executions
     * (i.e. Android) when the CPU goes to sleep.
     * <p/>
     * Calling {@code nudge} will cancel any pending network keepalives and fire one immediately.
     */
    void nudge();

    /**
     * You can Observe this event to capture signals that come in.
     * <p/>
     * As signals are always wrapped by SignalCommand this event and {@code onSignalCommandReceived} will fire simultaneously.
     */
    Observable<List<Signal>> getSignalReceivedEvent();

    /**
     * You can Observe this event to capture the higher level object containing the command
     * and the signal object.
     * <p/>
     * As signals are always wrapped by SignalCommand this event and {@code onSignalReceived} will fire simultaneously.
     */
    Observable<List<SignalCommand>> getSignalCommandReceivedEvent();

    /**
     * Observe the changes in connection. This is when your clientId is used for
     * the first time. True is connected and False is disconnected.
     * <p/>
     * This is a low level TCP connection observable.
     */
    Observable<Boolean> getConnectionChangedEvent();

    /**
     * Observe when we authenticate with the SignalServer. This means a new
     * clientId has been given to us for the first time. This should only fire
     * once.
     * <p/>
     * The String param is the clientId.
     */
    Observable<String> getNewClientIdReceivedEvent();

    /**
     * Observe when receive the subscription complete event indicating
     * that we are subscribed to the SignalServer and will begin to
     * receive events.
     */
    Observable<SubscriptionCompleteCommand> getSubscriptionCompleteReceivedEvent();

    /**
     * Observe when we receive a presence update and report if the phone is connected.
     * A True result indicates the phone is connected.
     */
    Observable<Boolean> getPhonePresenceReceivedEvent();

    /**
     * Observe a signal verification sent by another connected client.
     */
    Observable<Void> getSignalVerificationReceivedEvent();

    /**
     * Observe a new signal version for a given subscription and channel.
     */
    Observable<VersionMapEntry> getVersionChangedEvent();

    /**
     * Observe an inactive ping event.
     */
    Observable<PingEvent> getPingReceivedEvent();

    /**
     * Observe any exceptions in the {@code SignalConnection} layer.
     */
    Observable<String> getExceptionEvent();

    /**
     * Observe any commands sent from the Signal Server
     */
    Observable<Command> getCommandReceivedEvent();

    /**
     * Run on the connection thread if and only if by the time it actually runs the connection
     * has not changed state (ie: same clientId, etc). It also adds synchronization so that
     * the underlying connection cannot be changed while you are running. So PLEASE run very quickly.
     * No one can send/receive events or disconnect/reconnect while you are running.
     * <p/>
     * Be careful not to block within this method on any synchronization keywords. It would cause
     * deadlocks. IE: anything like future.get() on the disconnect/connect methods.
     *
     * @param runnable
     */
    ObservableFuture<Void> runIfActive(Runnable runnable);

    /**
     * Determines if the state is CONNECTED
     *
     * @return
     */
    boolean isConnected();

    /**
     * Determines if the state is AUTHENTICATED
     *
     * (isConnected() && has an active clientId)
     *
     * @return
     */
    boolean isAuthenticated();

}
