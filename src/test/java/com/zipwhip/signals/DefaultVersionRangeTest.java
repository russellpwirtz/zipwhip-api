package com.zipwhip.signals;

import com.zipwhip.timers.HashedWheelTimer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 10/4/13
 * Time: 11:47 AM
 */

public class DefaultVersionRangeTest {

    DefaultVersionRange versionRange = new DefaultVersionRange(new HashedWheelTimer());

   // TODO: Add unit test to check for the "single item" scenario

    @Test
    public void testPositiveHoleFilled() {

        versionRange.add(1);

        equals(Arrays.asList(1L));

        versionRange.add(2);

        equals(Arrays.asList(2L));

        versionRange.add(3);

        equals(Arrays.asList(3L));

        versionRange.add(5);

        equals(Arrays.asList(3L, 5L));

        versionRange.add(4);

        equals(Arrays.asList(5L));
    }

    @Test
    public void test2PositiveHoles() throws Exception {
        versionRange.add(1);

        equals(Arrays.asList(1L));

        versionRange.add(3);

        equals(Arrays.asList(1L, 3L));

        versionRange.add(5);

        equals(Arrays.asList(1L, 3L, 5L));

        versionRange.add(10);
        equals(Arrays.asList(1L, 3L, 5L, 10L));

        versionRange.add(11);
        equals(Arrays.asList(1L, 3L, 5L, 10L, 11L));

        versionRange.add(9L);
        equals(Arrays.asList(1L, 3L, 5L, 9L, 11L));

        versionRange.add(8L);
        equals(Arrays.asList(1L, 3L, 5L, 8L, 11L));

        versionRange.add(7L);
        equals(Arrays.asList(1L, 3L, 5L, 7L, 11L));

        versionRange.add(6L);
        equals(Arrays.asList(1L, 3L, 5L, 11L));

        versionRange.add(4L);
        equals(Arrays.asList(1L, 3L, 11L));
    }

    @Test
    public void testName() throws Exception {
        versionRange.add(4L);
        equals(Arrays.asList(4L));

        versionRange.add(1L);
        equals(Arrays.asList(1L, 4L));

        versionRange.add(2L);
        equals(Arrays.asList(2L, 4L));

        versionRange.add(3L);
        equals(Arrays.asList(4L));
    }

    @Test
    public void testRefillHoleTwice() throws Exception {
        versionRange.add(1);
        equals(Arrays.asList(1L));

        versionRange.add(3);
        equals(Arrays.asList(1L, 3L));

        versionRange.add(5);
        equals(Arrays.asList(1L, 3L, 5L));

        versionRange.add(10);
        equals(Arrays.asList(1L, 3L, 5L, 10L));

        versionRange.add(11);
        equals(Arrays.asList(1L, 3L, 5L, 10L, 11L));

        versionRange.add(9L);
        equals(Arrays.asList(1L, 3L, 5L, 9L, 11L));

        versionRange.add(8L);
        equals(Arrays.asList(1L, 3L, 5L, 8L, 11L));

        versionRange.add(7L);
        equals(Arrays.asList(1L, 3L, 5L, 7L, 11L));

        versionRange.add(6L);
        equals(Arrays.asList(1L, 3L, 5L, 11L));

        versionRange.add(6L);
        equals(Arrays.asList(1L, 3L, 5L, 11L));
    }

    private void equals(List<Long> values) {
        List<DefaultVersionRange.Value> list = new ArrayList<DefaultVersionRange.Value>();

        for (Long value : values) {
            list.add(new DefaultVersionRange.Value(value));
        }

        assertEquals(list, versionRange.list);
    }
}
