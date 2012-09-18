package com.zipwhip.api.signals;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionState;
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

    ConnectionState getConnectionState();

//    /**
//     * Determines if the state is CONNECTED
//     *
//     * @return
//     */
//    boolean isConnected();
//
//    /**
//     * Determines if the state is AUTHENTICATED
//     *
//     * (isConnected() && has an active clientId)
//     *
//     * @return
//     */
//    boolean isAuthenticated();

    /**
     * The SignalServer uses a separate id to track you, because it's an Id
     * given to a TCP/IP connection, not a user.
     *
     * @return the id that the SignalServer has for us
     */
    String getClientId();

    /**
     * Tell it to connect. This call is idempotent, so if multiple calls to
     * a connection provider will have no effect.
     *
     * @return a ObservableFuture task indicating if the connection was successful.
     * @throws Exception if an error is encountered when connecting
     */
    ObservableFuture<ConnectionHandle> connect();

    /**
     * Tell it to connect.
     *
     * @param clientId Pass in null if you don't have one.
     * @return A future that tells you when the connecting is complete. The
     *         string result is the clientId.
     * @throws Exception if an I/O happens while connecting
     */
    ObservableFuture<ConnectionHandle> connect(String clientId);

    /**
     * Tell it to connect.
     *
     * @param clientId Pass in null if you don't have one.
     * @param versions a Map of the current signal version per subscription.
     * @return A future that tells you when the connecting is complete. The
     *         string result is the clientId.
     * @throws Exception if an I/O happens while connecting.
     */
    ObservableFuture<ConnectionHandle> connect(String clientId, Map<String, Long> versions);

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
    ObservableFuture<ConnectionHandle> connect(String clientId, Map<String, Long> versions, Presence presence);

    /**
     * Tell it to disconnect.
     *
     * @return an event that tells you its complete
     * @throws Exception if an I/O happens while disconnecting
     */
    ObservableFuture<ConnectionHandle> disconnect();

    /**
     * Tell it to disconnect. Call this with false is equivalent to calling {@code disconnect()}.
     * Calling it with true will cause the connection to go into reconnect mode once the disconnect happens.
     *
     * @param causedByNetwork If the network state caused the disconnect
     * @return an event that tells you its complete
     * @throws Exception if an I/O happens while disconnecting
     */
    ObservableFuture<ConnectionHandle> disconnect(boolean causedByNetwork);

    /**
     * Will reset the state, followed by a disconnect, followed by an immediate reconnect.
     *
     * @throws Exception
     */
    ObservableFuture<ConnectionHandle> resetDisconnectAndConnect();

    /**
     * This little function is a BIG deal when you are running on a platform that freezes your executions
     * (i.e. Android) when the CPU goes to sleep.
     * <p/>
     * Calling {@code ping} will cancel any pending network keepalives and fire one immediately.
     */
    ObservableFuture<Boolean> ping();

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
     * Gets the currently connected ConnectionHandle
     *
     * @return
     */
    ConnectionHandle getConnectionHandle();

}
