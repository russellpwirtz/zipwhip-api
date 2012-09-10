package com.zipwhip.important;

import com.zipwhip.events.Observer;
import com.zipwhip.util.FutureDateUtil;
import com.zipwhip.util.HashCodeComparator;
import org.apache.log4j.Logger;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 9/4/12
 * Time: 12:50 PM
 */
public class SchedulerTimer implements Timer {

    private static final Logger LOGGER = Logger.getLogger(SchedulerTimer.class);

    private Scheduler scheduler;
    private Map<String, Timeout> map = Collections.synchronizedMap(new TreeMap<String, Timeout>(HashCodeComparator.getInstance()));

    @Override
    public Timeout newTimeout(final TimerTask task, long delay, TimeUnit unit) {

        final String requestId = UUID.randomUUID().toString();
        final Date exitDate = FutureDateUtil.inFuture(delay, unit);

        SchedulerTimeout timeout = new SchedulerTimeout(task, requestId, exitDate);

        LOGGER.debug(String.format("Scheduling requestId: %s for task: %s in %s", requestId, task, exitDate));

        map.put(requestId, timeout);

        scheduler.schedule(requestId, exitDate);

        return timeout;
    }

    @Override
    public synchronized Set<Timeout> stop() {

        LOGGER.error("SchedulerTimer stop()");

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
                LOGGER.warn("SchedulerTimer map was null!");
                return;
            }

            synchronized (this) {
                Timeout timeout = map.get(requestId);

                if (timeout == null) {
                    LOGGER.warn(String.format("timeout was null for requestId: %s", requestId));
                } else {
                    try {
                        TimerTask task = timeout.getTask();
                        if (task == null) {
                            LOGGER.warn("TimeoutTask was null!");
                        } else {
                            LOGGER.debug(String.format("Running requestId: %s task: %s!", requestId, task));
                            task.run(timeout);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Could not run task! ", e);
                    }
                }

                try {
                    LOGGER.debug(String.format("Done, removing requestId: %s! ", requestId));
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

    public void setScheduler(Scheduler scheduler) {

        if (this.scheduler != null) {
            throw new RuntimeException("We didn't handle this case.");
        }
        this.scheduler = scheduler;

        if (this.scheduler != null) {
            this.scheduler.onScheduleComplete(this.onTimeoutComplete);
        }
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
            return SchedulerTimer.this;
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
        }
    }

}
