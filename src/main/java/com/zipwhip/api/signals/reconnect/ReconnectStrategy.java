package com.zipwhip.api.signals.reconnect;

import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.DestroyableBase;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/31/11
 * Time: 10:43 AM
 *
 * Base class for {@code SignalConnection} reconnect strategies.
 */
public abstract class ReconnectStrategy extends DestroyableBase {

    private static final Logger LOGGER = Logger.getLogger(ReconnectStrategy.class);

    protected SignalConnection signalConnection;
    protected Observer<ConnectionHandle> disconnectObserver;

    private boolean isStarted;

    /**
     * If your connection is "connected" it does nothing. If your connection is "alive" but not "connected" it will
     * participate. It observes your "connection" events to determine when state changes.
     *
     * @param signalConnection The connection to manage.
     */
    public final void setSignalConnection(SignalConnection signalConnection) {

        if (isStarted()){
            stop();
        }

        this.signalConnection = signalConnection;
    }

    /**
     * Get the connection being managed.
     *
     * @return The managed connection.
     */
    public final SignalConnection getSignalConnection() {
        return signalConnection;
    }

    /**
     * Calling {@code stop} will cause this strategy to stop observing the {@code SignalConnection},
     * <p>
     * If the subclass implementation of {@code ReconnectStrategy} needs to do more work to shutdown cleanly you
     * should override this method as well as calling {@code super.stop()}.
     * </p>
     */
    public void stop() {

        LOGGER.debug("Stop reconnect strategy requested...");

        if (signalConnection != null && disconnectObserver != null) {
            signalConnection.getDisconnectEvent().removeObserver(disconnectObserver);
            isStarted = false;
        }
    }

    /**
     * Start participating in the {@code SignalConnection} connect/disconnect lifecycle.
     * This method is final as it handles the basic mechanics of binding to the {@code SignalConnection}.
     * To implement your strategy see {@code doStrategyWithoutBlocking}.
     */
    public synchronized final void start() {

        LOGGER.debug("Bind reconnect strategy requested...");

        if (!isStarted && signalConnection != null) {

            LOGGER.debug("Binding reconnect strategy to events...");

            disconnectObserver = new Observer<ConnectionHandle>() {

                @Override
                public void notify(Object sender, ConnectionHandle connectionHandle) {
                    boolean networkGenerated = connectionHandle.disconnectedViaNetwork();

                    // If the disconnect was generated due to a network problem we want to try a reconnect.
                    if (networkGenerated && isStarted) {
                        LOGGER.debug("Executing the doStrategyWithoutBlocking() method");
                        doStrategyWithoutBlocking();
                        LOGGER.debug("Done executing strategy!");
                    }
                }
            };

            signalConnection.getDisconnectEvent().addObserver(disconnectObserver);
//            signalConnection.onDisconnect(disconnectObserver);
            isStarted = true;
        }
    }

    /**
     * Has {@code start} been called and {@code stop} not been called subsequently.
     * @return {@code true} if the strategy is started.
     */
    public final boolean isStarted() {
        return isStarted;
    }

    /**
     * Put your strategy implementation here. This method will be called from {@code start} every time a reconnect event
     * fires from the {@code SignalConnection}. This method should not be called directly by clients.
     *
     * VERY IMPORTANT: DO NOT BLOCK OR SYNCHRONIZE ON ANYTHING IN THE CONNECTION!
     */
    protected abstract void doStrategyWithoutBlocking();

}
