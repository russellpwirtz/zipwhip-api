package com.zipwhip.signals;

import com.zipwhip.concurrent.HoleRange;
import com.zipwhip.events.Observer;
import com.zipwhip.timers.HashedWheelTimer;
import com.zipwhip.util.CollectionUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 10/4/13
 * Time: 11:47 AM
 */

public class DefaultVersionRangeTest {

    DefaultVersionRange versionRange;

    @Before
    public void setUp() throws Exception {
        versionRange = new DefaultVersionRange(new HashedWheelTimer(), 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testPositiveHoleFilled() {
        versionRange.add(1);
        assertListEquals(Arrays.asList(1L));
        assertHoleCount(0);
        versionRange.takeHoles();
        assertHoleCount(0);

        versionRange.add(2);
        assertListEquals(Arrays.asList(1L, 2L));
        assertHoleCount(0);
        versionRange.takeHoles();
        assertHoleCount(0);

        versionRange.add(3);
        assertListEquals(Arrays.asList(1L, 3L));
        assertHoleCount(0);
        versionRange.takeHoles();
        assertHoleCount(0);

        versionRange.add(5);
        assertListEquals(Arrays.asList(1L, 3L, 5L));
        assertHoleListEquals(Arrays.asList(4L));

        versionRange.add(4);
        assertListEquals(Arrays.asList(1L, 5L));
        assertHoleCount(0);
        versionRange.takeHoles();
        assertHoleCount(0);
    }

    @Test
    public void testMultiplePositiveHoles() throws Exception {
        versionRange.add(1);
        assertListEquals(Arrays.asList(1L));
        assertHoleCount(0);

        versionRange.add(3);
        assertListEquals(Arrays.asList(1L, 3L));
        assertHoleListEquals(Arrays.asList(2L));

        versionRange.add(5);
        assertListEquals(Arrays.asList(1L, 3L, 5L));
        assertHoleListEquals(Arrays.asList(2L, 4L));

        versionRange.add(10);
        assertListEquals(Arrays.asList(1L, 3L, 5L, 10L));
        assertHoleListEquals(Arrays.asList(2L, 4L, 6L, 7L, 8L, 9L));

        versionRange.add(11);
        assertListEquals(Arrays.asList(1L, 3L, 5L, 10L, 11L));
        assertHoleListEquals(Arrays.asList(2L, 4L, 6L, 7L, 8L, 9L));

        versionRange.add(9L);
        assertListEquals(Arrays.asList(1L, 3L, 5L, 9L, 11L));
        assertHoleListEquals(Arrays.asList(2L, 4L, 6L, 7L, 8L));

        versionRange.add(8L);
        assertListEquals(Arrays.asList(1L, 3L, 5L, 8L, 11L));
        assertHoleListEquals(Arrays.asList(2L, 4L, 6L, 7L));

        versionRange.add(7L);
        assertListEquals(Arrays.asList(1L, 3L, 5L, 7L, 11L));
        assertHoleListEquals(Arrays.asList(2L, 4L, 6L));

        versionRange.add(6L);
        assertListEquals(Arrays.asList(1L, 3L, 5L, 11L));
        assertHoleListEquals(Arrays.asList(2L, 4L));

        versionRange.add(4L);
        assertListEquals(Arrays.asList(1L, 3L, 11L));
        assertHoleListEquals(Arrays.asList(2L));

        versionRange.add(2L);
        assertListEquals(Arrays.asList(1L, 11L));
        assertHoleCount(0);
    }

    @Test
    public void testTriggerHoleObserver() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        versionRange.getHoleDetectedEvent().addObserver(new Observer<HoleRange>() {
            @Override
            public void notify(Object o, HoleRange holeRange) {
                latch.countDown();
            }
        });

        versionRange.add(1);
        assertListEquals(Arrays.asList(1L));
        assertHoleCount(0);

        versionRange.add(3);
        assertListEquals(Arrays.asList(1L, 3L));
        assertHoleListEquals(Arrays.asList(2L));

        versionRange.add(5);
        assertListEquals(Arrays.asList(1L, 3L, 5L));
        assertHoleListEquals(Arrays.asList(2L, 4L));

        assertTrue(CollectionUtil.exists(versionRange.holes));

        latch.await(2, TimeUnit.SECONDS);

        assertTrue(latch.getCount() == 0);
        assertTrue(CollectionUtil.isNullOrEmpty(versionRange.holes));
    }

    @Test
    public void testLargeInitialItem() throws Exception {
        versionRange.add(4L);
        assertListEquals(Arrays.asList(4L));
        assertHoleCount(0);

        versionRange.add(5L);
        assertListEquals(Arrays.asList(4L, 5L));
        assertHoleCount(0);

        versionRange.add(1L);
        assertListEquals(Arrays.asList(1L, 4L, 5L));
        assertHoleListEquals(Arrays.asList(2L, 3L));

        versionRange.add(2L);
        assertListEquals(Arrays.asList(1L, 2L, 4L, 5L));
        assertHoleListEquals(Arrays.asList(3L));

        versionRange.add(3L);
        assertListEquals(Arrays.asList(1L, 5L));
        assertHoleCount(0);
    }

    @Test
    public void testVersionReset() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        versionRange.getResetDetectedEvent().addObserver(new Observer<Long>() {
            @Override
            public void notify(Object o, Long version) {
                latch.countDown();
            }
        });

        versionRange.add(48L);
        assertListEquals(Arrays.asList(48L));
        assertHoleCount(0);

        versionRange.add(49L);
        assertListEquals(Arrays.asList(48L, 49L));
        assertHoleCount(0);

        versionRange.add(50L);
        assertListEquals(Arrays.asList(48L, 50L));
        assertHoleCount(0);

        versionRange.add(51L);
        assertListEquals(Arrays.asList(48L, 51L));
        assertHoleCount(0);

        versionRange.add(52L);
        assertListEquals(Arrays.asList(48L, 52L));
        assertHoleCount(0);

        // VERSION RESET!!
        versionRange.add(1L);
        assertListEquals(Arrays.asList(1L));
        assertHoleCount(0);

        versionRange.add(3L);
        assertListEquals(Arrays.asList(1L, 3L));
        assertHoleListEquals(Arrays.asList(2L));

        latch.await(5, TimeUnit.SECONDS);

        assertEquals(0L, latch.getCount());
    }

    @Test
    public void testRefillHoleTwice() throws Exception {
        versionRange.add(1);
        assertListEquals(Arrays.asList(1L));
        assertHoleCount(0);

        versionRange.add(3);
        assertListEquals(Arrays.asList(1L, 3L));
        assertHoleCount(1);

        versionRange.add(5);
        assertListEquals(Arrays.asList(1L, 3L, 5L));
        assertHoleCount(2);

        versionRange.add(6L);
        assertListEquals(Arrays.asList(1L, 3L, 5L, 6L));

        versionRange.add(6L);
        assertListEquals(Arrays.asList(1L, 3L, 5L, 6L));
    }

    private void assertHoleListEquals(List<Long> holeList) {
        assertTrue(holeList.size() == versionRange.holes.size());

        for (Long hole : versionRange.holes) {
            assertTrue(holeList.contains(hole));
        }
    }

    private void assertListEquals(List<Long> values) {
        List<DefaultVersionRange.Value> list = new ArrayList<DefaultVersionRange.Value>();

        for (Long value : values) {
            list.add(new DefaultVersionRange.Value(value));
        }

        assertEquals(list, versionRange.list);
    }

    private void assertHoleCount(int holes) {
        if (holes == 0) {
            assertTrue("Shouldn't have found holes!!", CollectionUtil.isNullOrEmpty(versionRange.holes));
        } else {
            assertTrue("Should have found holes!!", CollectionUtil.exists(versionRange.holes));

            assertEquals(holes, versionRange.holes.size());
        }
    }
}
