package com.zipwhip.concurrent;

import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;

import java.util.concurrent.Executor;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/8/12
 * Time: 11:36 AM
 *
 * For converting to a different executor.
 */
public class DifferentExecutorObserverAdapter<T> implements Observer<T> {

    final Observer<T> observer;
    final Executor executor;

    public DifferentExecutorObserverAdapter(Observer<T> observer) {
        this.executor = SimpleExecutor.getInstance();
        this.observer = observer;
    }

    public DifferentExecutorObserverAdapter(Executor executor, Observer<T> observer) {
        this.observer = observer;

        if (executor == null){
            executor = SimpleExecutor.getInstance();
        }
        this.executor = executor;
    }

    @Override
    public void notify(final Object sender, final T item) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                observer.notify(sender, item);
            }

            @Override
            public String toString() {
                return observer.toString();
            }
        });
    }
}
