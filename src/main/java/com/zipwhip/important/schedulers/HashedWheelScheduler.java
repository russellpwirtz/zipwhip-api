package com.zipwhip.important.schedulers;

import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.important.Scheduler;
import com.zipwhip.lifecycle.DestroyableBase;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 12:37 PM
 */
public class HashedWheelScheduler extends DestroyableBase implements Scheduler {

    final HashedWheelTimer timer;
    ObservableHelper<String> observableHelper = new ObservableHelper<String>();

    public HashedWheelScheduler() {
        this(null);
    }

    public HashedWheelScheduler(String name) {
        if (name == null) {
            timer = new HashedWheelTimer(new NamedThreadFactory("TimerManager-"), 1, TimeUnit.SECONDS);
        } else {
            timer = new HashedWheelTimer(new NamedThreadFactory("TimerManager-" + name + "-"), 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void schedule(final String requestId, Date exitTime) {
        timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                observableHelper.notifyObservers(HashedWheelScheduler.this, requestId);
            }
        }, exitTime.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
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
    protected void onDestroy() {
        timer.stop();
    }
}
