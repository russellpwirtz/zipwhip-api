package com.zipwhip.api.signals;

import java.io.Serializable;

/**
 * Date: 8/20/13
 * Time: 3:58 PM
 *
 * @author Michael
 * @version 1
 */
public class AsyncResult<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = -6404199227078710922L;

    private final T result;
    private final Throwable throwable;

    public AsyncResult(T result) {
        this.result = result;
        this.throwable = null;
    }

    public AsyncResult(Throwable throwable) {
        this.throwable = throwable;
        this.result = null;
    }

    public T getResult() {
        return result;
    }

    public Throwable getCause() {
        return throwable;
    }

    public boolean isSuccessful() {
        return throwable == null;
    }

    public boolean isFailed() {
        return !isSuccessful();
    }

}
