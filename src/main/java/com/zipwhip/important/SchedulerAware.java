package com.zipwhip.important;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/24/12
 * Time: 3:32 PM
 *
 * Something that is aware of the scheduler. We need this for our T extends XX & SchedulerAware stuff
 * in other projects.
 */
public interface SchedulerAware<T extends Scheduler> {

    T getScheduler();

}
