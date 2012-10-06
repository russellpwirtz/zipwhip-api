package com.zipwhip.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 10/5/12
 * Time: 5:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class MemoryKeyValueStore<K,V> implements KeyValueStore<K, V> {

    Map<K,V> map = Collections.synchronizedMap(new HashMap<K, V>());

    @Override
    public void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public void remove(K key) {
        map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
    }
}
