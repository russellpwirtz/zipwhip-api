package com.zipwhip.api.response;

/**
 * @author Michael
 */
public class KeyValuePair<T0, T1> {

    public T0 key;
    public T1 value;

    KeyValuePair(T0 key, T1 value) {
        this.key = key;
        this.value = value;
    }

}
