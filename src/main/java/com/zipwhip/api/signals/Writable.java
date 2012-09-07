package com.zipwhip.api.signals;

import com.zipwhip.concurrent.ObservableFuture;

/**
 * Created with IntelliJ IDEA.
 * User: msmyers
 * Date: 9/5/12
 * Time: 5:40 PM
 *
 * Allows you to write on a connection.
 */
public interface Writable {

    ObservableFuture<Boolean> write(Object object);

}
