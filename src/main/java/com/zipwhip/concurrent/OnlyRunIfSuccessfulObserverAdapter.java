package com.zipwhip.concurrent;

import com.zipwhip.events.Observer;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/10/12
 * Time: 6:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class OnlyRunIfSuccessfulObserverAdapter<T> extends ObserverAdapter<ObservableFuture<T>> {

    private static final Logger LOGGER = Logger.getLogger(OnlyRunIfSuccessfulObserverAdapter.class);

    public OnlyRunIfSuccessfulObserverAdapter(Observer<ObservableFuture<T>> observer) {
        super(observer);
    }

    @Override
    public void notify(Object sender, ObservableFuture<T> future) {
        synchronized (future) {
            if (future.isSuccess()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("Successful, so notifying %s of %s", getObserver(), future));
                }

                super.notify(sender, future);
            } else {
                LOGGER.warn(String.format("Not successful, so not notifying %s of %s", getObserver(), future));
            }
        }
    }
}
