package com.zipwhip.concurrent;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/11/12
 * Time: 3:31 PM
 *
 * For creating things that take in an input.
 */
public interface ConfiguredFactory<TConfiguration, TProduces> {

    TProduces create(TConfiguration configuration);

}
