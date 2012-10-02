package com.zipwhip.util;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/13/12
 * Time: 5:36 PM
 *
 * Extend this to adapt different stores.
 */
public abstract class KeyValueStoreAdapter<KExternal,KInternal,V> implements KeyValueStore<KExternal,V> {

    private final KeyValueStore<KInternal, V> store;

    public KeyValueStoreAdapter(KeyValueStore<KInternal, V> store) {
        this.store = store;
    }

    @Override
    public void put(KExternal key, V value) {
        store.put(getKey(key), value);
    }

    @Override
    public V get(KExternal key) {
        return store.get(getKey(key));
    }

    protected abstract KInternal getKey(KExternal key);

    @Override
    public void remove(KExternal key) {
        store.remove(getKey(key));
    }

    @Override
    public void clear() {
        store.clear();
    }
}
