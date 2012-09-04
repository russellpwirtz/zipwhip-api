package com.zipwhip.api.signals;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.Destroyable;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

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
     * Initiate a raw TCP connection to the signal server ASYNCHRONOUSLY. This
     * is just a raw connection, not an authenticated/initialized one.
     *
     * @return The future will tell you when the connection is complete.
     * @throws Exception if there is is an error connecting
     */
    Future<Boolean> connect() throws Exception;

    /**
     * Kill the TCP connection to the SignalServer ASYNCHRONOUSLY
     *
     * @return The future will tell you when the connection is terminated,
     * @throws Exception if there is is an error disconnecting
     */
    Future<Void> disconnect() throws Exception;

    /**
     * Kill the TCP connection to the SignalServer ASYNCHRONOUSLY. Pass true if the disconnect
     * is being ordered as a result of a network problem such as a stale connection or a channel
     * disconnect.
     *
     * @param network True if the disconnect results from a problem on the network.
     * @return The future will tell you when the connection is terminated,
     * @throws Exception if there is is an error disconnecting
     */
    Future<Void> disconnect(boolean network) throws Exception;

    /**
     * Cancel any pending network keepalives and fire one immediately.
     */
    void keepalive();

    /**
     * Send something to the SignalServer
     *
     * @param command the Command to send
     * @return a {@code Future} of type boolean where true is a successful send.
     */
    ObservableFuture<Boolean> send(SerializingCommand command);

    /**
     * Determines if the socket is currently connected
     *
     * @return returns true if connected, false if not connected.
     */
    boolean isConnected();

    /**
     * Allows you to listen for things that are received by the API.
     *
     * @param observer An observer to receive callbacks on when this event fires
     */
    void onMessageReceived(Observer<Command> observer);

    /**
     * Allows you to observe the connection trying to connect.
     * The observer will return True if the connection was successful, False otherwise.
     *
     * @param observer An observer to receive callbacks on when this event fires
     */
    void onConnect(Observer<Boolean> observer);

    /**
     * Allows you to observe the connection disconnecting.
     * The observer will return True if a reconnect is requested, False otherwise.
     *
     * @param observer An observer to receive callbacks on when this event fires
     */
    void onDisconnect(Observer<Boolean> observer);

    /**
     * Allows you to stop observing the connection trying to connect.
     *
     * @param observer An observer to stop receiving callbacks on.
     */
    void removeOnConnectObserver(Observer<Boolean> observer);

    /**
     * Allows you to stop observing the connection trying to connect.
     *
     * @param observer An observer to stop receiving callbacks on.
     */
    void removeOnMessageReceivedObserver(Observer<Command> observer);

    /**
     * Allows you to stop observing the disconnection trying to connect.
     *
     * @param observer An observer to stop receiving callbacks on.
     */
    void removeOnDisconnectObserver(Observer<Boolean> observer);

    /**
     * Observe an inactive ping event.
     *
     * @param observer An Observer of type PingEvent to indicate the event that happened.
     */
    void onPingEvent(Observer<PingEvent> observer);

    /**
     * Observe a caught exception in the connection.
     *
     * @param observer An Observer of type String to indicating the exception that was caught.
     */
    void onExceptionCaught(Observer<String> observer);

    /**
     * Get the host being used to connect to Zipwhip.
     *
     * @return the host being used to connect to Zipwhip.
     */
    String getHost();

    /**
     * Set the host to be used on the NEXT connection.
     * If not set the default SignalServer host will be used.
     *
     * @param host the host to be used on the NEXT connection
     */
    void setHost(String host);

    /**
     * Get the port being used to connect to Zipwhip.
     *
     * @return the port being used to connect to Zipwhip.
     */
    int getPort();

    /**
     * Sets the port to be used on the NEXT connection.
     * If not set the default SignalServer port will be used.
     *
     * @param port the port to be used on the NEXT connection
     */
    void setPort(int port);

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

    /**
     * Runs the specified runnable on the core connection thread. It will only run if-only-if the
     * current channel on enqueue matches the current channel on execution. This allows you to do
     * such things as call disconnect on the current connection ensuring with 100% certainty that
     * you are connecting the right channel instead of a new one.
     *
     * @param callable
     */
    public <T> ObservableFuture<T> runIfActive(Callable<T> callable);

    /**
     * Will run on the core Connection executor. This will prevent any changes to the underlying
     * connection while you are running.
     *
     * @param callable
     * @param <T>
     * @return
     */
    public <T> ObservableFuture<T> runSafely(Callable<T> callable);

    /**
     * This is how you tell that the underlying connection hasn't changed. If you do a connect/disconnect/reconnect or
     * lose the channel, this connectionId will increment.
     *
     * @return
     */
    long getConnectionId();
}
