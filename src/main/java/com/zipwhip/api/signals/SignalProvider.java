package com.zipwhip.api.signals;

import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.signals.presence.UserAgent;

/**
 * Date: 5/7/13
 * Time: 4:41 PM
 *
 * SignalProvider is the only class you need to talk with the signal server. The old design broke up the
 * /signals/connect and clientId work into 2 classes.
 *
 * @author Michael Smyers
 * @version 2
 */
public interface SignalProvider extends Destroyable {

    /**
     * Tell it to connect. This call is idempotent, so if multiple calls to
     * a connection provider (if already connected) will have no effect.
     *
     * If you do not have a presence object set on this, it will fail.
     *
     * You must have UserAgent information defined. If you call this method without userAgent information, it will crash.
     *
     * @throws IllegalStateException If you do not have userAgent information previously defined.
     * @return a ObservableFuture task indicating if the connection was successful.
     * @throws Exception if an error is encountered when connecting
     */
    ObservableFuture<Void> connect() throws IllegalStateException;

    /**
     * Tell it to connect. This call is idempotent, so if multiple calls to
     * a connection provider (if already connected) will have no effect.
     *
     * If you do not have a presence object set on this, it will fail.
     *
     * You must have UserAgent information defined.
     *
     * @throws IllegalStateException If you do not have userAgent information defined.
     * @return a ObservableFuture task indicating if the connection was successful.
     * @throws Exception if an error is encountered when connecting
     */
    ObservableFuture<Void> connect(UserAgent userAgent) throws IllegalStateException;

    /**
     * Tell it to connect. This call is idempotent, so if multiple calls to
     * a connection provider (if already connected) will have no effect.
     *
     * If you do not have a presence object set on this, it will fail.
     *
     * You must have UserAgent information defined.
     *
     * @throws IllegalStateException If you do not have userAgent information defined.
     * @return a ObservableFuture task indicating if the connection was successful.
     * @throws Exception if an error is encountered when connecting
     */
    ObservableFuture<Void> connect(UserAgent userAgent, String clientId) throws IllegalStateException;

    /**
     * Tell it to disconnect. Will not reconnect. Equivalent to .disconnect(false);
     *
     * @return A future that can be observed
     * @throws Exception if an I/O happens while disconnecting
     */
    ObservableFuture<Void> disconnect();

    /**
     * Tell it to disconnect. Call this with false is equivalent to calling {@code disconnect()}.
     * Calling it with true will cause the connection to go into reconnect mode once the disconnect happens.
     *
     * @param causedByNetwork If the network state caused the disconnect
     * @return an event that tells you its complete
     * @throws Exception if an I/O happens while disconnecting
     */
    ObservableFuture<Void> disconnect(boolean causedByNetwork);

    /**
     * Bind a sessionKey+subscriptionId to a clientId. The server will respond with a SubscriptionCompleteCommand.
     *
     * The server will remember this binding permanently. You only need to do this during the initial login phase
     * of your app.
     *
     * @param sessionKey
     * @param subscriptionId
     * @return
     */
    ObservableFuture<SubscribeResult> subscribe(String sessionKey, String subscriptionId);

    /**
     *
     *
     * @param subscriptionId
     * @return
     */
    ObservableFuture<SubscribeResult> unsubscribe(String sessionKey, String subscriptionId);

    /**
     * Will reset the state, followed by a disconnect, followed by an immediate reconnect.
     *
     * @throws Exception
     */
    ObservableFuture<Void> resetDisconnectAndConnect();

    Observable<SubscribeResult> getSubscribeEvent();

    Observable<SubscribeResult> getUnsubscribeEvent();

    Observable<DeliveredMessage> getMessageReceivedEvent();

    /**
     * The presence information that this client is conveying to cloud.
     *
     * @return
     */
    UserAgent getUserAgent();

    /**
     * The SignalServer uses a separate id to track you, because it's an Id
     * given to a TCP/IP connection, not a user.
     *
     * The server gives out clientIds
     *
     * @return the id that the SignalServer has for us
     */
    String getClientId();

}