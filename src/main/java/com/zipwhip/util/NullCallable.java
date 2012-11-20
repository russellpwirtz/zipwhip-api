package com.zipwhip.util;

import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 11/14/12
 * Time: 3:53 PM
 *
 * Always returns null
 */
public class NullCallable implements Callable {

    private static final NullCallable INSTANCE = new NullCallable();

    @Override
    public Object call() throws Exception {
        return null;
    }

    public static NullCallable getInstance() {
        return INSTANCE;
    }
}
