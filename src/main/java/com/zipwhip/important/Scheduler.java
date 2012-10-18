package com.zipwhip.important;

import com.zipwhip.events.Observer;

import java.lang.String;import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 12:07 PM
 *
 * This class allows for scheduling of things that go beyond the destruction of the JVM. The scheduler's job
 * is to persist timeouts even if the JVM reboots. A typical implementation would need to persist each token
 * to disk and resume on start.
 */
public interface Scheduler {

    void schedule(String requestId, Date exitTime);

    void scheduleRecurring(String requestId, long interval, TimeUnit units);

    void cancel(String requestId);

    void onScheduleComplete(Observer<String> observer);

    void removeOnScheduleComplete(Observer<String> observer);

}
