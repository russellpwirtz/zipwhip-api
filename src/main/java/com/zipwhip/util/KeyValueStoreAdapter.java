package com.zipwhip.util;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/13/12
 * Time: 5:36 PM
 *
 * Extend this to adapt different stores.
 */
public class KeyValueStoreAdapter<K,V> implements KeyValueStore<K,V> {

    private final KeyValueStore<K, V> store;

    public KeyValueStoreAdapter(KeyValueStore<K, V> store) {
        this.store = store;
    }

    @Override
    public void put(K key, V value) {
        store.put(key, value);
    }

    @Override
    public V get(K key) {
        return store.get(key);
    }

    @Override
    public void remove(K key) {
        store.remove(key);
    }

    @Override
    public void clear() {
        store.clear();
    }
}
