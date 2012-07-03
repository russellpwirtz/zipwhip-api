package com.zipwhip.api.signals.sockets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 7/2/12
 * Time: 2:00 PM
 */
public class FlexibleTimedEvictionMapTest {

    FlexibleTimedEvictionMap<Integer, Integer> map;

    static final int DEFAULT_IDEAL_SIZE = 5;
    static final long DEFAULT_MIN_EVICTION_AGE = 100;

    @Before
    public void setUp() throws Exception {
        map = new FlexibleTimedEvictionMap<Integer, Integer>(DEFAULT_IDEAL_SIZE, DEFAULT_MIN_EVICTION_AGE);
        map.put(0, 0);
        map.put(1, 1);
        map.put(2, 2);
        map.put(3, 3);
        map.put(4, 4);
    }

    @Test
    public void testBasicShrinkQueue() throws Exception {
        Thread.sleep(101);
        Assert.assertEquals(5, map.size());
        map.shrink();
        Assert.assertEquals(5, map.size());
        map.put(5, 5);
        map.put(6, 6);
        map.shrink();
        Assert.assertEquals(6, map.size());
        map.shrink();
        Assert.assertEquals(5, map.size());
    }

    @Test
    public void testTimedShrinkQueue() throws Exception {
        Assert.assertEquals(5, map.size());
        map.shrink();
        Assert.assertEquals(5, map.size());
        map.put(5, 5);
        map.put(6, 6);
        map.shrink();
        Assert.assertEquals(7, map.size()); // 7 since we haven't expired
        Thread.sleep(101);
        map.shrink();
        Assert.assertEquals(6, map.size());
        map.shrink(2);
        Assert.assertEquals(5, map.size());
    }

    @Test
    public void testBasicShrinkQueueNumber() throws Exception {
        Thread.sleep(101);
        Assert.assertEquals(5, map.size());
        map.shrink(2);
        Assert.assertEquals(5, map.size());
        map.put(5, 5);
        map.put(6, 6);
        map.shrink(2);
        Assert.assertEquals(5, map.size());
    }

    @Test
    public void testGetIdealSize() throws Exception {
        Assert.assertEquals(DEFAULT_IDEAL_SIZE, map.getIdealSize());
    }

    @Test
    public void testSetIdealSize() throws Exception {
        map.setIdealSize(100);
        Assert.assertEquals(100, map.getIdealSize());
    }

    @Test
    public void testGetMinimumEvictionAgeMillis() throws Exception {
        Assert.assertEquals(100l, map.getMinimumEvictionAgeMillis());
    }

    @Test
    public void testSetMinimumEvictionAgeMillis() throws Exception {
        map.setMinimumEvictionAgeMillis(1000);
        Assert.assertEquals(1000l, map.getMinimumEvictionAgeMillis());
    }

}
