package com.zipwhip.lib;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/31/11
 * Time: 5:21 PM
 *
 * An interface to settingsStore key, value data
 *
 */
public interface KeyValueStore<K,V> {

    /**
     * Put a key, value in the settingsStore.
     * Implementations should update the data if the key already exists.
     *
     * @param key Your key.
     * @param value Your value.
     */
    void put(K key, V value);

    /**
     * Get a value based on a key if it exists.
     *
     * @param key Your key
     * @return The corresponding value if it exists.
     */
    V get(K key);

    /**
     * Clear the key from the settingsStore.
     *
     * @param key the key to remove
     */
    void remove(K key);

    /**
     * Clear all keys from the settingsStore.
     */
    void clear();

}
