package com.zipwhip.util;

import com.zipwhip.api.signals.Signal;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 10/3/12
 * Time: 1:37 PM
 *
 * Use when u want to convert the signal
 */
public class SignalContentConverter<T> implements Converter<Signal, T>{

    private static final SignalContentConverter INSTANCE = new SignalContentConverter();

    @Override
    public T convert(Signal signal) throws Exception {
        return (T)signal.getContent();
    }

    @Override
    public Signal restore(T t) throws Exception {
        return null;
    }

    public static SignalContentConverter getInstance() {
        return INSTANCE;
    }
}
