package com.zipwhip.api.signals;

import org.apache.log4j.Logger;

import java.util.Date;
import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/15/11
 * Time: 10:29 AM
 * <p/>
 * This class schedules reconnect attempts in a geometrically increasing (2^X by default) way up to a threshold.
 * Once the threshold has been reached all subsequent reconnect attempts will be run at the threshold time.
 * <p/>
 * If an reconnect has been scheduled but has not completed any subsequent disconnect notices will be ignored.
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

    private long maxBackoffSeconds;
    private double multiplier;
    private long consecutiveReconnectAttempts;

    private ScheduledExecutorService exec;
    private Future<Boolean> reconnectTask;
    private ScheduledFuture<?> scheduledTask;
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

        // If we have scheduled a reconnect cancel it
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }

        // If we have scheduled a reconnect cancel it
        if (reconnectTask != null && !reconnectTask.isDone()) {
            reconnectTask.cancel(false);
        }

        super.stop();
    }

    @Override
    protected void doStrategy() {

//        signalConnection.onConnect(new Observer<Boolean>() {
//            @Override
//            public void notify(Object sender, Boolean connected) {
//
//                if (connected) {
//                    // We connected, reset
//                    consecutiveReconnectAttempts = 0;
//                }
//            }
//        });

        if (exec == null) {
            exec = Executors.newSingleThreadScheduledExecutor();
        }

        if (reconnectRunnable == null) {
            return;
        }

        if (scheduledTask != null && !scheduledTask.isDone()) {
            // We have a reconnect scheduled, drop this one
            return;
        }

        if (reconnectRunnable == null) {

            reconnectRunnable = new Runnable() {
                @Override
                public void run() {
                    try {

                        LOGGER.debug("Connect attempt at =============================================>> " + new Date(System.currentTimeMillis()));

                        reconnectTask = signalConnection.connect();
                        Boolean success = reconnectTask.get();

                        if (success) {

                            LOGGER.debug("Reconnected successfully");

                            // We connected, reset
                            consecutiveReconnectAttempts = 0;

                        } else {

                            LOGGER.warn("Error reconnecting, disconnecting...");

                            signalConnection.disconnect(true);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error reconnecting", e);
                    }
                }
            };
        }

        try {

            LOGGER.debug("Scheduling attempt at =============================================>> " + new Date(System.currentTimeMillis()));

            scheduledTask = exec.schedule(reconnectRunnable, calculateBackoff(), TimeUnit.SECONDS);
            consecutiveReconnectAttempts++;

        } catch (Exception e) {

            LOGGER.error(e);
        }
    }

    @Override
    protected void onDestroy() {
        stop();
    }

    private long calculateBackoff() {

        long backoff = Math.round(Math.pow(multiplier, consecutiveReconnectAttempts));

        LOGGER.debug("Backoff calculated as =============================================>> " + backoff);

        return (backoff > maxBackoffSeconds ? maxBackoffSeconds : backoff);
    }

}
