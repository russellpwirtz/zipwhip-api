package com.zipwhip.api.signals;

import com.zipwhip.api.signals.commands.PresenceCommand;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.events.Observer;
import com.zipwhip.signals.presence.Presence;

import javax.security.auth.Destroyable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

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
     * Set the clientId. NOTE: This will only have an effect if you set it
     * BEFORE calling connect.
     * 
     * @param clientId
     */
    void setClientId(String clientId);

    /**
     * Get a Presence that was previously set.
     *
     * @return Presence previously set
     */
    public Presence getPresence();

    /**
     * Set Presence to be used on the NEXT connection
     *
     * @param presence Presence to be used on the Next connection.
     */
    public void setPresence(Presence presence);

    /**
     * Tell it to connect.
     *
     * @return
     * @throws Exception
     */
    Future<Boolean> connect() throws Exception;

    /**
     * Tell it to connect.
     * 
     * @param clientId
     *        Pass in null if you don't have one.
     * @return A future that tells you when the connecting is complete. The
     *         string result is the clientId.
     * @throws Exception if an I/O happens while connecting
     */
    Future<Boolean> connect(String clientId) throws Exception;

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

    Future<Boolean> connect(String clientId, Map<String, Long> versions) throws Exception;

    /**
     * Tell it to disconnect.
     * 
     * @return an event that tells you its complete
     * @throws Exception if an I/O happens while disconnecting
     */
    Future<Void> disconnect() throws Exception;

    /**
     * You can Observe this event to capture things that come through
     * 
     * @param observer
     */
    void onSignalReceived(Observer<List<Signal>> observer);

    /**
     * Observe the changes in connection. This is when your clientId is used for
     * the first time.
     * 
     * This is a low level TCP connection observable.
     * 
     * @param observer
     */
    void onConnectionChanged(Observer<Boolean> observer);

    /**
     * Observe when we authenticate with the SignalServer. This means a new
     * clientId has been given to us for the first time. This should only fire
     * once.
     * 
     * @param observer
     */
    void onNewClientIdReceived(Observer<String> observer);

    /**
     * Observe when receive the subscription complete event indicating
     * that we are subscribed to the SignalServer and will begin to
     * receive events.
     *
     * @param observer
     */
    void onSubscriptionComplete(Observer<SubscriptionCompleteCommand> observer);

    /**
     * Observe when we receive a presence update.
     *
     * @param observer
     */
    void onPresenceReceived(Observer<PresenceCommand> observer);

    /**
     * Observe a signal verification sent by another connected client.
     *
     * @param observer
     */
    void onSignalVerificationReceived(Observer<Void> observer);

}
