package com.zipwhip.api.signals;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.lifecycle.Destroyable;
import java.net.SocketAddress;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/2/11 Time: 11:48 AM
 * <p/>
 * Encapsulates a connection to the signal server. This is a very LOW LEVEL
 * interface for talking with the SignalServer.
 * <p/>
 * It's not really intended for the callers to interact with this API directly.
 */
public interface SignalConnection extends Destroyable {

    /**
     * Determines if the socket is currently connected
     *
     * @return returns true if connected, false if not connected.
     */
    boolean isConnected();

    ConnectionHandle getCurrentConnection();

    /**
     * Initiate a raw TCP connection to the signal server ASYNCHRONOUSLY. This
     * is just a raw connection, not an authenticated/initialized one.
     *
     * @return The future will tell you when the connection is complete.
     * @throws Exception if there is is an error connecting
     */
    ObservableFuture<ConnectionHandle> connect();

    /**
     * Kills the current connection and reconnects to a new connection within one safe operation/transaction. Will not allow other
     * operations to occur between those two actions.
     *
     * @return
     */
    ObservableFuture<ConnectionHandle> reconnect();

    /**
     * Kill the TCP connection to the SignalServer ASYNCHRONOUSLY
     *
     * @return The future will tell you when the connection is terminated, (Connection that was disconnected)
     * @throws Exception if there is is an error disconnecting
     */
    ObservableFuture<ConnectionHandle> disconnect();

    /**
     * Kill the TCP connection to the SignalServer ASYNCHRONOUSLY. Pass true if the disconnect
     * is being ordered as a result of a network problem such as a stale connection or a channel
     * disconnect.
     *
     * @param network True if the disconnect results from a problem on the network.
     * @return The future will tell you when the connection is terminated, (Connection that was disconnected)
     * @throws Exception if there is is an error disconnecting
     */
    ObservableFuture<ConnectionHandle> disconnect(boolean network);

    /**
     * Cancel any pending network keepalives and fire one immediately.
     */
    void ping();

    /**
     * Send something to the SignalServer
     *
     * @param command the Command to send
     * @return a {@code Future} of type boolean where true is a successful send.
     */
    ObservableFuture<Boolean> send(SerializingCommand command);

    /**
     * Allows you to observe the connection trying to connect.
     * The observer will return True if the connection was successful, False otherwise.
     */
    Observable<ConnectionHandle> getConnectEvent();

    /**
     * Allows you to observe the connection disconnecting.
     * The observer will return True if a reconnect is requested, False otherwise.
     *
     * @return observer An observer to receive callbacks on when this event fires
     */
    Observable<ConnectionHandle> getDisconnectEvent();

    /**
     * Allows you to listen for things that are received by the API.
     */
    Observable<Command> getCommandReceivedEvent();

    /**
     * Observe an inactive ping event.
     */
    Observable<PingEvent> getPingEventReceivedEvent();

    /**
     * Observe a caught exception in the connection.
     */
    Observable<String> getExceptionEvent();

    /**
     * The address of the server to connect to.
     *
     * @param address
     */
    void setAddress(SocketAddress address);
    SocketAddress getAddress();

    /**
     * Get the current reconnection strategy for the connection.
     *
     * @return the current reconnection strategy for the connection.
     */
    ReconnectStrategy getReconnectStrategy();

    /**
     * Set the reconnection strategy for the connection.
     *
     * @param strategy the strategy to use when reconnecting
     */
    void setReconnectStrategy(ReconnectStrategy strategy);


    /**
     * Set the current setting for a connection time out in seconds.
     *
     * @param connectTimeoutSeconds The setting for a connection time out in seconds.
     */
    void setConnectTimeoutSeconds(int connectTimeoutSeconds);

    /**
     * Get the current setting for a connection time out in seconds.
     *
     * @return The current setting for a connection time out in seconds.
     */
    int getConnectTimeoutSeconds();

}
