package com.zipwhip.lib;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/31/11
 * Time: 5:21 PM
 *
 * An interface to store key, value data
 *
 */
public interface KeyValueStore<K,V> {

    /**
     * Put a key, value in the store.
     * Implementations should update the data if the key already exists.
     *
     * @param key Your key
     * @param value Your value
     */
    void put(K key, V value);

    /**
     * Get a value based on a key or an empty String if the key doesn't exist.
     *
     * @param key Your key
     * @return The corresponding value or empty String.
     */
    V get(K key);

    /**
     * Clear the key from the store.
     *
     * @param key the key to clear
     */
    void clear(K key);

    /**
     * Clear all keys from the store.
     */
    void clearAll();

}
