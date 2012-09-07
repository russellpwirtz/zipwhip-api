package com.zipwhip.api.signals.reconnect;

import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class schedules reconnect attempts in a geometrically increasing (2^X by default) way up to a threshold.
 * Once the threshold has been reached all subsequent reconnect attempts will be run at the threshold time.
 * <p/>
 * If a reconnect has been scheduled but has not completed any subsequent disconnect notices will be ignored.
 */
public class ExponentialBackoffReconnectStrategy extends ReconnectStrategy {

    private static final Logger LOGGER = Logger.getLogger(ExponentialBackoffReconnectStrategy.class);

    /**
     * The default maximum backoff time (10 minutes)
     */
    public static final long DEFAULT_MAX_BACKOFF_SECONDS = 10 * 60;

    /**
     * The default 'multiplier' value is 2 (100% increase per backoff)
     */
    public static final double DEFAULT_MULTIPLIER = 2.0;

    private static final long DEFUALT_DELAY = 2;

    private static final long INITIAL_ATTEMPTS = 1;
    private TimeUnit delayUnits = TimeUnit.SECONDS;
    private boolean connectObserverSet;

    private long maxBackoffSeconds = DEFAULT_MAX_BACKOFF_SECONDS;
    private double multiplier;
    private long consecutiveReconnectAttempts = INITIAL_ATTEMPTS;

    private ScheduledExecutorService scheduler;
    private ObservableFuture<ConnectionHandle> reconnectTask;
    private Runnable reconnectRunnable;

    public ExponentialBackoffReconnectStrategy() {
        this(DEFAULT_MAX_BACKOFF_SECONDS);
    }

    public ExponentialBackoffReconnectStrategy(long maxBackoffSeconds) {
        super();
        this.multiplier = DEFAULT_MULTIPLIER;
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    /**
     * Returns the current value of exponential 'multiplier' i.e. the exponential base.
     *
     * @return The current value of exponential 'multiplier'
     */
    public double getMultiplier() {
        return multiplier;
    }

    /**
     * Set the multiplier value. Values much in excess of the default, 2.0 will grow the backoff very fast.
     *
     * @param multiplier The value to use as the exponential base when calculating backoff intervals.
     */
    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public void stop() {

        cleanup();

        super.stop();
    }

    @Override
    protected synchronized void doStrategyWithoutBlocking() {

        // Start listening for connect events to reset our counter
        if (!connectObserverSet) {

            signalConnection.getConnectEvent().addObserver(new Observer<ConnectionHandle>() {

                @Override
                public void notify(Object sender, ConnectionHandle connectionHandle) {
                    // todo: is isActive the same as isConnected?
                    boolean connected = !connectionHandle.isDestroyed();

                    if (connected) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("We reconnected, cleaning up and resetting count.");
                        }

                        // Cancel any scheduled reconnects
                        cleanup();

                        // We connected, reset
                        consecutiveReconnectAttempts = INITIAL_ATTEMPTS;
                    }
                }
            });

            connectObserverSet = true;
        }

        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("ReconnectStrategy-"));
        }

        if (reconnectRunnable == null) {

            reconnectRunnable = new Runnable() {
                @Override
                public void run() {
                    try {

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Connect attempt at ==>> " + new Date(System.currentTimeMillis()));
                        }

                        consecutiveReconnectAttempts++;
                        reconnectTask = signalConnection.connect();

                    } catch (Exception e) {

                        LOGGER.error("Error reconnecting", e);
                    }
                }
            };
        }

        try {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Scheduling attempt at ==>> " + new Date(System.currentTimeMillis()));
            }

            scheduler.schedule(reconnectRunnable, calculateBackoff(), delayUnits);

        } catch (Exception e) {

            LOGGER.error(e);
        }
    }

    @Override
    protected void onDestroy() {
        stop();
    }

    protected long calculateBackoff() {

        long backoff = Math.max(DEFUALT_DELAY, Math.round(Math.pow(multiplier, consecutiveReconnectAttempts)));

        backoff = (backoff > maxBackoffSeconds ? maxBackoffSeconds : backoff);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Backoff calculated as ==>> " + backoff + (backoff == 1 ? " second" : " seconds"));
        }

        return backoff;
    }

    private void cleanup() {
        // If we have scheduled a reconnect cancel it
        if ((reconnectTask != null) && !reconnectTask.isDone()) {

            boolean cancelled = reconnectTask.cancel(true);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Cancelling reconnect task success: " + cancelled);
            }
        }

        // Cleanup any scheduled reconnects
        if (scheduler != null) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Shutting down scheduled execution");
            }

            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /**
     * @return the delayUnits
     */
    public final TimeUnit getDelayUnits() {
        return delayUnits;
    }

    /**
     * @param delayUnits the delayUnits to set
     */
    public final void setDelayUnits(TimeUnit delayUnits) {
        this.delayUnits = delayUnits;
    }

    /**
     * @param consecutiveReconnectAttempts the consecutiveReconnectAttempts to set
     */
    protected final void setConsecutiveReconnectAttempts(long consecutiveReconnectAttempts) {
        this.consecutiveReconnectAttempts = consecutiveReconnectAttempts;
    }

}
