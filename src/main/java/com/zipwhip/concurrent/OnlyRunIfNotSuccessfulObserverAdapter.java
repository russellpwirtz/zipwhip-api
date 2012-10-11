package com.zipwhip.concurrent;

import com.zipwhip.events.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * So we can do conditionals with constructors
 *
 * @param <T>
 */
public class OnlyRunIfNotSuccessfulObserverAdapter<T> implements Observer<ObservableFuture<T>> {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnlyRunIfNotSuccessfulObserverAdapter.class);

    final Observer<ObservableFuture<T>> observer;

    public OnlyRunIfNotSuccessfulObserverAdapter(Observer<ObservableFuture<T>> observer) {
        this.observer = observer;
    }

    @Override
    public void notify(Object sender, ObservableFuture<T> item) {
        if (!item.isSuccess()) {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace(String.format("Running %s/%s/%s", observer, sender, item));
            observer.notify(sender, item);
        } else {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Did not notify inner observer because successful. " + item);
        }
    }

}
