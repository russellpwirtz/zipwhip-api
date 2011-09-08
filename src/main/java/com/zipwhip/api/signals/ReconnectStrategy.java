package com.zipwhip.api.signals;

import com.zipwhip.events.Observer;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/31/11
 * Time: 10:43 AM
 *
 * Base class for {@code SignalConnection} reconnect strategies.
 */
public abstract class ReconnectStrategy {

    protected SignalConnection signalConnection;
    protected Observer<Boolean> disconnectObserver;

    private boolean isStarted;

    public ReconnectStrategy(SignalConnection signalConnection) {
        this.signalConnection = signalConnection;
    }

    /**
     * If your connection is "connected" it does nothing. If your connection is "alive" but not "connected" it will
     * participate. It observes your "signalConnection" events to determine when state changes.
     *
     * @param signalConnection The connection to manage.
     */
    public void setSignalConnection(SignalConnection signalConnection) {
        this.signalConnection = signalConnection;
    }

    /**
     * Get the connection being managed.
     *
     * @return The managed connection.
     */
    public SignalConnection getSignalConnection() {
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
        if (signalConnection != null && disconnectObserver != null) {

            signalConnection.removeOnDisconnectObserver(disconnectObserver);
            isStarted = false;
        }
    }

    /**
     * Start participating in the {@code SignalConnection} connect/disconnect lifecycle.
     * This method is final as it handles the basic mechanics of binding to the {@code SignalConnection}.
     * To implement your strategy see {@code doStrategy}.
     */
    public final void start() {
        if (!isStarted && signalConnection != null) {

            disconnectObserver = new Observer<Boolean>() {

                @Override
                public void notify(Object sender, Boolean reconnect) {

                    if (reconnect) {
                        doStrategy();
                    }
                }
            };

            signalConnection.onDisconnect(disconnectObserver);

            isStarted = true;
        }
    }

    /**
     * Has {@code start} been called and {@code stop} not been called subsequently.
     * @return True if the strategy is started, otherwise False.
     */
    public final boolean isStarted() {
        return isStarted;
    }

    /**
     * Put your strategy implementation here. This method will be called from {@code start} every time a reconnect event
     * fires from the {@code SignalConnection}. This method should not be called directly by clients.
     */
    protected abstract void doStrategy();

}
