package com.zipwhip.concurrent;

import com.zipwhip.api.NestedObservableFuture;
import com.zipwhip.events.Observer;

/**
* Created with IntelliJ IDEA.
* User: Michael
* Date: 9/9/12
* Time: 5:19 PM
* To change this template use File | Settings | File Templates.
*/
public class CascadeStateObserver<T, TOther> implements Observer<ObservableFuture<TOther>> {

    private final ObservableFuture<T> future;
    private final T result;

    public CascadeStateObserver(ObservableFuture<T> future, T result) {
        this.future = future;
        this.result = result;
    }

    @Override
    public void notify(Object sender, ObservableFuture<TOther> item) {
        NestedObservableFuture.syncState(item, future, result);
    }
}
