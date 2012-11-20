package com.zipwhip.concurrent;

import com.zipwhip.events.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/8/12
 * Time: 12:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class DebugObserver<T> implements Observer<ObservableFuture<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugObserver.class);

    @Override
    public void notify(Object sender, ObservableFuture<T> item) {
        if (item.isCancelled()) {
            LOGGER.warn(String.format("%s was cancelled by %s", item, Thread.currentThread()));
        } else if (item.isFailed()) {
            LOGGER.warn(String.format("%s was failed by %s with %s", item, Thread.currentThread(), item.getCause()));
        } else if (item.isSuccess()) {
            LOGGER.warn(String.format("%s was successed by %s with %s", item, Thread.currentThread(), item.getResult()));
        }
    }
}
