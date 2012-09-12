package com.zipwhip.api.signals.reconnect;

import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.reliable.retry.ExponentialBackoffRetryStrategy;
import com.zipwhip.reliable.retry.RetryStrategy;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.FutureDateUtil;
import org.apache.log4j.Logger;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

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

    private ObservableFuture<ConnectionHandle> connectFuture;
    private Timeout timeout;
    private RetryStrategy strategy;
    private final Timer timer;
    private int failCount;

    public DefaultReconnectStrategy() {
        this(null);
    }

    public DefaultReconnectStrategy(Timer timer) {
        this(timer, null);
    }

    public DefaultReconnectStrategy(Timer timer, RetryStrategy strategy) {
        super();

        if (timer == null) {
            timer = new HashedWheelTimer(new NamedThreadFactory(this.toString()), 1, TimeUnit.SECONDS);
        }
        this.timer = timer;

        if (strategy == null) {
            strategy = new ExponentialBackoffRetryStrategy(1, 2.0);
        }
        this.strategy = strategy;
    }

    @Override
    public synchronized void stop() {
        cleanup();

        // Stop listening to SignalConnection events
        super.stop();
    }

    @Override
    protected synchronized void doStrategyWithoutBlocking() {
        long delay = getReconnectDelay();
        LOGGER.debug(String.format("Scheduling a reconnect attempt to occur on %s", FutureDateUtil.inFuture(delay, TimeUnit.MILLISECONDS)));
        timeout = timer.newTimeout(reconnectTimerTask, delay, TimeUnit.MILLISECONDS);
    }

    private long getReconnectDelay() {
        return strategy.getNextRetryInterval(failCount);
    }

    private final Observer<ObservableFuture<ConnectionHandle>> incrementFailCountOnCompleteObserver = new Observer<ObservableFuture<ConnectionHandle>>() {
        @Override
        public void notify(Object sender, ObservableFuture<ConnectionHandle> future) {
            synchronized (DefaultReconnectStrategy.this) {
                if (future.isSuccess()) {
                    failCount = 0;
                } else {
                    failCount++;
                }
            }
        }
    };

    private TimerTask reconnectTimerTask = new TimerTask() {
        @Override
        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled()) {
                return; //WARNING: is this the right thing to do?
            }

            synchronized (DefaultReconnectStrategy.this) {
                cancelExistingConnectFuture();

                connectFuture = signalConnection.connect();
                connectFuture.addObserver(incrementFailCountOnCompleteObserver);
            }
        }
    };

    protected synchronized void cleanup() {
        cancelExistingConnectFuture();
        cancelExistingTimeout();
    }

    private synchronized void cancelExistingConnectFuture() {
        if (connectFuture == null) {
            return;
        }

        // If we have scheduled a reconnect cancel it
        if (!connectFuture.isDone()) {
            boolean cancelled = connectFuture.cancel(true);
            LOGGER.debug("Cancelling reconnect task success: " + cancelled);
        }

        connectFuture = null;
    }

    private synchronized void cancelExistingTimeout() {
        if (timeout == null) {
            return;
        }

        LOGGER.debug("stop() called, Shutting down scheduled execution");
        timeout.cancel();
        timeout = null;
    }

    @Override
    protected void onDestroy() {
        stop();
    }

    @Override
    public String toString() {
        return "DefaultReconnectStrategy";
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }
}
