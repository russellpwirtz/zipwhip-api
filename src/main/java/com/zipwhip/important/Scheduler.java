package com.zipwhip.important;

import com.zipwhip.events.Observer;

import java.lang.String;import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 12:07 PM
 */
public interface Scheduler {

    void schedule(String requestId, Date exitTime);

    void onScheduleComplete(Observer<String> observer);

    void removeOnScheduleComplete(Observer<String> observer);

}
