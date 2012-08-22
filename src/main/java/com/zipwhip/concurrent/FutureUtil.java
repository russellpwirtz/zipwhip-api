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
public class FutureUtil {

    public static <T> Future<T> execute(Executor executor, Callable<T> callable) {
        if (executor == null){
            throw new IllegalArgumentException("The executor can't be null!");
        }

        FutureTask<T> task = new FutureTask<T>(callable);

        executor.execute(task);

        return task;
    }

    public static <T> ObservableFuture<T> execute(Executor executor, final Object sender, final Callable<T> callable) {
        if (executor == null){
            throw new IllegalArgumentException("The executor can't be null!");
        }

        final ObservableFuture<T> future = new DefaultObservableFuture<T>(sender);

        executor.execute(new Runnable() {
            @Override
            public void run() {

                T result;

                try {
                    result = callable.call();
                } catch (Exception e) {
                    future.setFailure(e);
                    return;
                }
                future.setSuccess(result);
            }
        });

        return future;
    }

    public static <T> ObservableFuture<T> execute(Executor executor, final Object sender, final Future<T> future) {
        if (executor == null){
            throw new IllegalArgumentException("The executor can't be null!");
        }

        final ObservableFuture<T> result = new DefaultObservableFuture<T>(sender);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    result.setSuccess(future.get());
                } catch (Exception e) {
                    result.setFailure(e);
                }
            }
        });

        return result;
    }

}
