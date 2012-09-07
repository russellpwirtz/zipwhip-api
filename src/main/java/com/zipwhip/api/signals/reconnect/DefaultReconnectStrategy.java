package com.zipwhip.api.signals.reconnect;

import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import org.apache.log4j.Logger;

import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/7/11
 * Time: 2:29 PM
 * <p/>
 * Try to reconnect the SignalConnection every 5 seconds until we get a successful connection.
 */
public class DefaultReconnectStrategy extends ReconnectStrategy {

    private static final Logger LOGGER = Logger.getLogger(DefaultReconnectStrategy.class);

    private static final long RECONNECT_DELAY = 5000;

    private ObservableFuture<ConnectionHandle> reconnectTask;
    private ScheduledExecutorService scheduler;

    @Override
    public void stop() {

        // If we have scheduled a reconnect cancel it
        if (reconnectTask != null && !reconnectTask.isDone()) {

            boolean cancelled = reconnectTask.cancel(true);

            LOGGER.debug("Cancelling reconnect task success: " + cancelled);
        }

        // Cleanup any scheduled reconnects
        if (scheduler != null) {

            LOGGER.debug("stop() called, Shutting down scheduled execution");

            scheduler.shutdownNow();
            scheduler = null;
        }

        // Stop listening to SignalConnection events
        super.stop();
    }

    @Override
    protected void doStrategyWithoutBlocking() {

        LOGGER.debug("Scheduling a reconnect attempt in 5 seconds...");

        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("ReconnectStrategy-"));
        }

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    LOGGER.debug("Attempting a reconnect...");
                    reconnectTask = signalConnection.connect();
                } catch (Exception e) {
                    LOGGER.error("Error reconnecting", e);
                }
            }
        }, RECONNECT_DELAY, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onDestroy() {
        stop();
    }

}
