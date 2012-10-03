package com.zipwhip.important;

import com.zipwhip.events.Observer;
import com.zipwhip.important.schedulers.TimerScheduler;
import com.zipwhip.timers.Timeout;
import com.zipwhip.timers.Timer;
import com.zipwhip.timers.TimerTask;
import com.zipwhip.util.FutureDateUtil;
import com.zipwhip.util.HashCodeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 9/4/12
 * Time: 12:50 PM
 */
public class ZipwhipSchedulerTimer implements Timer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipwhipSchedulerTimer.class);

    private Map<String, Timeout> map = Collections.synchronizedMap(new TreeMap<String, Timeout>(HashCodeComparator.getInstance()));

    private final Scheduler scheduler;

    public ZipwhipSchedulerTimer(Scheduler scheduler) {
        if (scheduler == null){
            this.scheduler = new TimerScheduler("SchedulerTimer");
        } else {
            this.scheduler = new ScopedScheduler(scheduler) {
                @Override
                public String toString() {
                    return String.format("[SchedulerTimer: %s]", ZipwhipSchedulerTimer.this);
                }
            };
        }

        this.scheduler.onScheduleComplete(this.onTimeoutComplete);
    }

    @Override
    public Timeout newTimeout(final TimerTask task, long delay, TimeUnit unit) {

        final String requestId = UUID.randomUUID().toString();
        final Date exitDate = FutureDateUtil.inFuture(delay, unit);

        SchedulerTimeout timeout = new SchedulerTimeout(task, requestId, exitDate);

        map.put(requestId, timeout);

        scheduler.schedule(requestId, exitDate);

        return timeout;
    }

    @Override
    public synchronized Set<Timeout> stop() {

        LOGGER.debug("SchedulerTimer stop()");

        Set set = new TreeSet<Timeout>(HashCodeComparator.getInstance());
        set.addAll(map.values());

        map.clear();
        map = null;

        return set;

    }

    private final Observer<String> onTimeoutComplete = new Observer<String>() {
        @Override
        public void notify(Object sender, String requestId) {

            if (map == null) {
                LOGGER.error("SchedulerTimer map was null!");
                return;
            }

            synchronized (this) {
                Timeout timeout = map.get(requestId);
                if (timeout == null) {
                    LOGGER.warn(String.format("Timeout was null for requestId: %s", requestId));
                    return;
                }

                try {
                    TimerTask task = timeout.getTask();
                    if (task == null) {
                        LOGGER.error("TimeoutTask was null!");
                    } else {
                        LOGGER.debug(String.format("Running requestId: %s task: %s!", requestId, task));
                        task.run(timeout);
                    }
                } catch (Exception e) {
                    LOGGER.error("Could not run task! ", e);
                }

                try {
                    map.remove(requestId);
                } catch (Exception e) {
                    LOGGER.error("Exception removing timeout from scheduler map! ", e);
                }
            }
        }
    };

    public Scheduler getScheduler() {
        return scheduler;
    }

    private class SchedulerTimeout implements Timeout {

        private boolean isCancelled;
        private TimerTask task;
        private Date exitDate;
        private String requestId;

        private SchedulerTimeout(TimerTask task, String requestId, Date exitDate) {
            this.requestId = requestId;
            this.task = task;
            this.exitDate = exitDate;
        }

        @Override
        public Timer getTimer() {
            return ZipwhipSchedulerTimer.this;
        }
        @Override
        public TimerTask getTask() {
            return task;
        }

        @Override
        public boolean isExpired() {
            return FutureDateUtil.isExpired(exitDate);
        }

        @Override
        public boolean isCancelled() {
            return isCancelled;
        }

        @Override
        public synchronized void cancel() {
            isCancelled = true;
            scheduler.cancel(this.requestId);
        }
    }

}
