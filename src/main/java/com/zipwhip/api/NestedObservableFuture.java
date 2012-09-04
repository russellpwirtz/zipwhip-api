package com.zipwhip.api;

import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;

import java.util.concurrent.Executor;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/30/12
 * Time: 11:51 AM
 */
public class NestedObservableFuture<T> extends DefaultObservableFuture<T> {

    protected ObservableFuture<T> nestedFuture;
    boolean nesting = false;
    boolean alreadySyncedStateWithNestedFuture = false;

    public NestedObservableFuture(Object sender) {
        super(sender);
    }

    public NestedObservableFuture(Object sender, Executor executor) {
        super(sender, executor);
    }

    public ObservableFuture<T> getNestedFuture() {
        return nestedFuture;
    }

    public synchronized void setNestedFuture(final ObservableFuture<T> nestedFuture) {
        this.nesting = true;
        if (this.nestedFuture != null) {
            throw new RuntimeException("We were lazy and didnt implement this scenario.");
        }

        this.nestedFuture = nestedFuture;

        synchronized (nestedFuture) {
            this.addObserver(new CopyFutureStatusToNestedFuture<T>(this.nestedFuture));
            this.nestedFuture.addObserver(new CopyFutureStatusToNestedFuture<T>(this));

            // Note: we believe that even if the observableFutures are asynchronous, the above lines
            // are sufficient.
//            syncState(this.nestedFuture, this);
//            syncState(this, this.nestedFuture);
        }

        this.nesting = false;
        this.alreadySyncedStateWithNestedFuture = true;
    }

    @Override
    public synchronized boolean setFailure(Throwable cause) {
        boolean wasAChange = super.setFailure(cause);

        if (wasAChange && this.alreadySyncedStateWithNestedFuture) {
            // i need to tell my child.
            this.nestedFuture.setFailure(cause);
        }

        return wasAChange;
    }

    @Override
    public synchronized boolean setSuccess(T result) {
        boolean wasAChange = super.setSuccess(result);

        if (wasAChange && this.alreadySyncedStateWithNestedFuture) {
            // i need to tell my child.
            this.nestedFuture.setSuccess(result);
        }

        return wasAChange;
    }

    @Override
    public synchronized boolean cancel() {
        boolean wasAChange = super.cancel();

        if (wasAChange && this.alreadySyncedStateWithNestedFuture) {
            // i need to tell my child.
            this.nestedFuture.cancel();
        }

        return wasAChange;
    }

    @Override
    public boolean cancel(boolean b) {
        boolean wasAChange = super.cancel(b);

        if (wasAChange && this.alreadySyncedStateWithNestedFuture) {
            // i need to tell my child.
            this.nestedFuture.cancel(b);
        }

        return wasAChange;
    }

    @Override
    public String toString() {
        if (nestedFuture != null) {
            return "Nested/" + nestedFuture.toString();
        } else {
            return "NestedObservableFuture";
        }
    }

    public static <T> void syncState(ObservableFuture<T> source, ObservableFuture<T> destination) {
        if (source.isDone()) {
            if (source.isCancelled()) {
                destination.cancel();
            } else if (source.isSuccess()) {
                destination.setSuccess(source.getResult());
            } else {
                destination.setFailure(source.getCause());
            }
        }
    }

    public static <T> void syncStateBoolean(ObservableFuture<T> source, ObservableFuture<Boolean> destination) {
        if (source.isDone()) {
            if (source.isCancelled()) {
                destination.cancel();
            } else if (source.isSuccess()) {
                destination.setSuccess(Boolean.TRUE);
            } else if (source.getCause() != null) {
                destination.setFailure(source.getCause());
            } else {
                throw new RuntimeException("How did this get here? What state are we missing? " + source);
            }
        }
    }
}
