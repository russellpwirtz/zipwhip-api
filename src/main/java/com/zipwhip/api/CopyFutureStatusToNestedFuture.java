package com.zipwhip.api;

import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import org.apache.log4j.Logger;

/**
 * When the future finishes, if it's the current "connectFuture" clean up the references.
 */
public class CopyFutureStatusToNestedFuture<T> implements Observer<ObservableFuture<T>> {

    private final static Logger LOGGER = Logger.getLogger(CopyFutureStatusToNestedFuture.class);

    final ObservableFuture<T> nestedFuture;

    public CopyFutureStatusToNestedFuture(ObservableFuture<T> nestedFuture) {
        this.nestedFuture = nestedFuture;
    }

    @Override
    public void notify(Object sender, ObservableFuture<T> future) {
        LOGGER.trace(String.format("Cloning the state from %s to %s", future, nestedFuture));
        // notify people that care.
        NestedObservableFuture.syncState(future, nestedFuture);
    }
}
