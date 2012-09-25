package com.zipwhip.api;

import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import org.apache.log4j.Logger;

/**
 * When the future finishes, if it's the current "connectFuture" clean up the references.
 */
public class CopyFutureStatusToNestedFutureWithCustomResult<T1, T2> implements Observer<ObservableFuture<T1>> {

    private final static Logger LOGGER = Logger.getLogger(CopyFutureStatusToNestedFutureWithCustomResult.class);

    final ObservableFuture<T2> nestedFuture;
    final T2 result;

    public CopyFutureStatusToNestedFutureWithCustomResult(ObservableFuture<T2> nestedFuture, T2 result) {
        this.nestedFuture = nestedFuture;
        this.result = result;
    }

    @Override
    public void notify(Object sender, ObservableFuture<T1> future) {
        LOGGER.trace(String.format("Cloning the state from %s to %s", future, nestedFuture));
        // notify people that care.
        NestedObservableFuture.syncState(future, nestedFuture, result);
    }
}
