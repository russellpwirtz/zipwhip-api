package com.zipwhip.api.signals.sockets;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 6/29/12
 * Time: 4:18 PM
 *
 * This
 */
public class FlexibleTimedEvictionMap<K, V> extends TreeMap<K, V> {

    // The ideal size of the window. The window can grow larger within a given time period.
    private int idealSize;

    // The minimum time in milliseconds before an item will be evicted.
    private long minimumEvictionAgeMillis;

    // Hold a map of minimum eviction times keyed to the same keys as this
    private final Map<K, Long> evictionTimeMap = new HashMap<K, Long>();

    public FlexibleTimedEvictionMap(int idealSize, long minimumEvictionAgeMillis) {
        super();
        this.idealSize = idealSize;
        this.minimumEvictionAgeMillis = minimumEvictionAgeMillis;
    }

    @Override
    public V put(K k, V v) {
        evictionTimeMap.put(k, System.currentTimeMillis());
        return super.put(k, v);
    }

    @Override
    public void clear() {
        evictionTimeMap.clear();
        super.clear();
    }

    /**
     * Equivalent to calling {@code shrink(1)}
     */
    public void shrink() {
        shrink(1);
    }

    /**
     * Attempt to shrink the map by a number of nodes starting with the lowest ordered node.
     * If the map has less nodes than {@code idealSize} or if the nodes to be removed have not
     * expired then they will not be removed.
     *
     * This method is a hint to get the map to resize.
     *
     * @param size The number of nodes to attempt to shrink the map by.
     */
    public void shrink(int size) {

        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < size; i++) {

            if (size() > idealSize) {

                K key = firstKey();

                if (currentTime - evictionTimeMap.get(key) > minimumEvictionAgeMillis) {
                    remove(key);
                    evictionTimeMap.remove(key);
                }
            } else {
                break;
            }
        }

    }

    public int getIdealSize() {
        return idealSize;
    }

    public void setIdealSize(int idealSize) {
        this.idealSize = idealSize;
    }

    public long getMinimumEvictionAgeMillis() {
        return minimumEvictionAgeMillis;
    }

    public void setMinimumEvictionAgeMillis(long minimumEvictionAgeMillis) {
        this.minimumEvictionAgeMillis = minimumEvictionAgeMillis;
    }

}
