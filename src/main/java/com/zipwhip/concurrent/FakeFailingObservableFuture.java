package com.zipwhip.concurrent;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/30/12
 * Time: 3:12 PM
 */
public class FakeFailingObservableFuture<V> extends DefaultObservableFuture<V> {

    public FakeFailingObservableFuture(Object sender, Throwable ex) {
        super(sender);

        this.setFailure(ex);
    }

}
