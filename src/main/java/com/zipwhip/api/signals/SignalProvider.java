package com.zipwhip.api.signals;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
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
    boolean isConnected();

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
     * @param clientId
     *        Pass in null if you don't have one.
     * @return A future that tells you when the connecting is complete. The
     *         string result is the clientId.
     * @throws Exception if an I/O happens while connecting
     */
    ObservableFuture<Boolean> connect(String clientId) throws Exception;

    /**
     * Tell it to connect.
     *
     * @param clientId
     *        Pass in null if you don't have one.
     * @param versions a Map of the current signal version per subscription.
     * @return A future that tells you when the connecting is complete. The
     *         string result is the clientId.
     * @throws Exception if an I/O happens while connecting.
     */
    ObservableFuture<Boolean> connect(String clientId, Map<String, Long> versions) throws Exception;

    /**
     * Tell it to connect. The future will unblock when {action:"CONNECT"} comes back
     *
     * @param clientId
     *        Pass in null if you don't have one.
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
     *
     * Calling {@code nudge} will cancel any pending network keepalives and fire one immediately.
     */
    void nudge();

    /**
     * You can Observe this event to capture signals that come in.
     *
     * As signals are always wrapped by SignalCommand this event and {@code onSignalCommandReceived} will fire simultaneously.
     * 
     * @param observer an Observer of type List<Signal> to listen for new signal events.
     */
    void onSignalReceived(Observer<List<Signal>> observer);

    /**
     * You can Observe this event to capture the higher level object containing the command
     * and the signal object.
     *
     * As signals are always wrapped by SignalCommand this event and {@code onSignalReceived} will fire simultaneously.
     *
     * @param observer an Observer of type Map<Long, Signal> to listen for new signal events.
     */
    void onSignalCommandReceived(Observer<List<SignalCommand>> observer);

    /**
     * Observe the changes in connection. This is when your clientId is used for
     * the first time. True is connected and False is disconnected.
     * 
     * This is a low level TCP connection observable.
     * 
     * @param observer an Observer of type Boolean to listen for connection changes events.
     */
    void onConnectionChanged(Observer<Boolean> observer);

    /**
     * Observe when we authenticate with the SignalServer. This means a new
     * clientId has been given to us for the first time. This should only fire
     * once.
     *
     * The String param is the clientId.
     * 
     * @param observer an Observer of type String to listen for the event.
     */
    void onNewClientIdReceived(Observer<String> observer);

    /**
     * Observe when receive the subscription complete event indicating
     * that we are subscribed to the SignalServer and will begin to
     * receive events.
     *
     * @param observer an Observer of type SubscriptionCompleteCommand to listen for the event.
     */
    void onSubscriptionComplete(Observer<SubscriptionCompleteCommand> observer);

    /**
     * Observe when we receive a presence update and report if the phone is connected.
     * A True result indicates the phone is connected.
     *
     * @param observer an Observer of type Boolean to listen for the event.
     */
    void onPhonePresenceReceived(Observer<Boolean> observer);

    /**
     * Observe a signal verification sent by another connected client.
     *
     * @param observer an Observer of type Void to listen for the event.
     */
    void onSignalVerificationReceived(Observer<Void> observer);

    /**
     * Observe a new signal version for a given subscription and channel.
     *
     * @param observer an Observer of type  to listen for the event.
     */
    void onVersionChanged(Observer<VersionMapEntry> observer);

    /**
     * Observe an inactive ping event.
     *
     * @param observer an Observer of type PingEvent indicating the event that happened.
     */
    void onPingEvent(Observer<PingEvent> observer);

    /**
     * Observe any exceptions in the {@code SignalConnection} layer.
     *
     * @param observer an Observer of type String indicating the exception that occurred.
     */
    void onExceptionEvent(Observer<String> observer);

    /**
     * Observe any commands sent from the Signal Server
     *
     * @param observer an Observer of type Command indicating the command that was sent.
     */
    void onCommandReceived(Observer<Command> observer);

    /**
     * Run on the connection thread if and only if by the time it actually runs the connection
     * has not changed state (ie: same clientId, etc). It also adds synchronization so that
     * the underlying connection cannot be changed while you are running. So PLEASE run very quickly.
     * No one can send/receive events or disconnect/reconnect while you are running.
     *
     * Be careful not to block within this method on any synchronization keywords. It would cause
     * deadlocks. IE: anything like future.get() on the disconnect/connect methods.
     *
     * @param runnable
     */
    ObservableFuture<Void> runIfActive(Runnable runnable);

    void removeOnSubscriptionCompleteObserver(Observer<SubscriptionCompleteCommand> observer);

    void removeOnConnectionChangedObserver(Observer<Boolean> observer);

}
