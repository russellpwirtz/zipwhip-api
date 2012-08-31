package com.zipwhip.api.signals.sockets;

import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;
import org.apache.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
* Created by IntelliJ IDEA.
* User: Russ
* Date: 8/31/12
* Time: 4:04 PM
*/
class BlockingExecutorObserver<T> implements Observer<T> {

    private final static Logger LOGGER = Logger.getLogger(BlockingExecutorObserver.class);

    private final Observer<T> observer;
    private final Object sender;
    private final Executor executor;

    public BlockingExecutorObserver(Object sender, Executor executor, Observer<T> observer) {
        this.observer = observer;
        this.sender = sender;
        this.executor = executor;
    }

    public BlockingExecutorObserver(Executor executor, Observer<T> observer) {
        this(null, executor, observer);
    }

    public BlockingExecutorObserver(Observer<T> observer) {
        this(SimpleExecutor.getInstance(), observer);
    }

    @Override
    public void notify(Object sender, final T item) {
        final Object s;
        if (this.sender != null) {
            s = this.sender;
        } else {
            s = sender;
        }

        LOGGER.debug(String.format("[%s: Notify() called for observer %s in %s", this, observer, executor));

        final CountDownLatch latch = new CountDownLatch(1);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    observer.notify(s, item);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public String toString() {
                return "BlockingExecutorObserver/runnable/" + observer;
            }
        });

        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                LOGGER.error(String.format("[%s: Latch never finished for observer %s in %s", this, observer, executor));
            }
        } catch (InterruptedException e) {
            LOGGER.error("Exception during latch wait", e);
        }
    }

    @Override
    public String toString() {
        return "BlockingExecutorObserver/" + observer;
    }
}
