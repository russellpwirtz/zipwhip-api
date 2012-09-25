package com.zipwhip.concurrent;

import com.zipwhip.util.Asserts;
import com.zipwhip.util.Directory;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/8/12
 * Time: 4:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class SafetyBoundary<T extends Enum> {

    private Map<Access, Directory<T, Object>> locks;

    enum Access {
        /**
         * You want to read the value and have it not change.
         */
        READ,

        /**
         * You want to write a new value.
         */
        WRITE,

        /**
         * You want to change something on the value.
         */
        MODIFY
    }

    public void add(Access access, T key, Object object) {
        locks.get(access).add(key, object);
    }

    protected void ensureAbleTo(Access access, T key) {
        Collection<Object> locks = this.locks.get(access).get(key);

        for (Object lock : locks) {
            ensureLocked(lock, key, access);
        }
    }

    private void ensureLocked(Object lock, T key, Access access) {
        Asserts.assertTrue(Thread.holdsLock(lock), String.format("The thread must hold the lock on %s for %s with ensureAbleTo %s", lock, key, access));
    }

    public void modify(T key, Object thingy) {
        // you should have been able to read this.
        ensureAbleTo(Access.READ, key);
        // you should have been able to modify this.
        ensureAbleTo(Access.MODIFY, key);
        ensureLocked(thingy, key, Access.MODIFY);
    }

}
