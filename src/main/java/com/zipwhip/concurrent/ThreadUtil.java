package com.zipwhip.concurrent;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/8/12
 * Time: 11:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadUtil {

    public static void ensureLock(Object lock) {
        if (lock == null) {
            return;
        }

        if (!Thread.holdsLock(lock)) {
            throw new IllegalStateException("Do not hold lock that is required: " + lock);
        }
    }

    public static void ensureThread(Thread thread) {
        if (Thread.currentThread() != thread) {
            throw new IllegalAccessError(String.format("Current thread was supposed to be %s but was %s", thread, Thread.currentThread()));
        }
    }
}
