package com.zipwhip.important;

import com.zipwhip.concurrent.ObservableFuture;import java.lang.Exception;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 11:13 AM
 */
public interface Worker<TParameters, TResponse> {

    ObservableFuture<TResponse> execute(TParameters parameters) throws Exception;

}
