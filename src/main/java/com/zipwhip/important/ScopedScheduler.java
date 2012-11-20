package com.zipwhip.important;

import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 9/7/12
 * Time: 5:01 PM
 *
 * Does not cascade destruction to the underlying scheduler
 */
public class ScopedScheduler extends CascadingDestroyableBase implements Scheduler, Observer<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScopedScheduler.class);

    private final Scheduler scheduler;
    private final String name;
    private final ObservableHelper<String> observableHelper;

    public ScopedScheduler(Scheduler scheduler) {
        this(scheduler, UUID.randomUUID().toString());
    }

    public ScopedScheduler(Scheduler scheduler, String prefix) {
        this.scheduler = scheduler;
        this.name = prefix;
        this.scheduler.onScheduleComplete(this);
        this.observableHelper = new ObservableHelper<String>(name);
    }

    @Override
    public void schedule(String requestId, Date exitTime) {
        scheduler.schedule(name + requestId, exitTime);
    }

    @Override
    public void scheduleRecurring(String requestId, long interval, TimeUnit units) {
        scheduler.scheduleRecurring(name + requestId, interval, units);
    }

    @Override
    public void cancel(String requestId) {
        scheduler.cancel(name + requestId);
    }

    @Override
    public void onScheduleComplete(Observer<String> observer) {
        observableHelper.addObserver(observer);
    }

    @Override
    public void removeOnScheduleComplete(Observer<String> observer) {
        observableHelper.removeObserver(observer);
    }

    @Override
    public void notify(Object sender, String item) {
        String key = item.replace(name, "");
        if (StringUtil.equalsIgnoreCase(key, item)) {
            // no change? return, not for us.
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("(%s) Ignoring %s because it was not our scope.", this, item));
            }
            return;
        }

        observableHelper.notifyObservers(sender, key);
    }

    @Override
    public String toString() {
        return String.format("[ScopedScheduler: %s]", name);
    }

    @Override
    protected void onDestroy() {

    }
}
