package com.zipwhip.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/16/12
 * Time: 12:38 PM
 *
 * Convenience methods for creating/working with futures.
 */
public class Futures {

    public static <T> Future<T> execute(Executor executor, Callable<T> callable) {
        if (executor == null){
            throw new IllegalArgumentException("The executor can't be null!");
        }

        FutureTask<T> task = new FutureTask<T>(callable);

        executor.execute(task);

        return task;
    }

}
