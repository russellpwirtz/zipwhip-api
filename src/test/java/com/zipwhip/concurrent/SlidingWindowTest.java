package com.zipwhip.concurrent;

import com.zipwhip.events.Observer;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 6/25/12
 * Time: 4:43 PM
 */
public class SlidingWindowTest {

    SlidingWindow<Long> window;
    String key = "channel:/1234-5678-9012";

    static final int DEFAULT_WINDOW_SIZE = 2;
    static final long DEFAULT_MIN_EXPIRATION = 100;

    @Before
    public void setUp() throws Exception {
        window = new SlidingWindow<Long>(null, key, DEFAULT_WINDOW_SIZE, DEFAULT_MIN_EXPIRATION);
    }

    @Test
    public void testGetKey() throws Exception {
        Assert.assertEquals(key, window.getKey());
    }

    @Test
    public void testSetKey() throws Exception {
        String newKey = "new/key";
        window.setKey(newKey);
        Assert.assertEquals(newKey, window.getKey());
    }

    @Test
    public void testGetSeed() throws Exception {
        Assert.assertEquals(SlidingWindow.INITIAL_CONDITION, window.getIndexSequence());
    }

    @Test
    public void testSetSeed() throws Exception {
        window.setIndexSequence(2L);
        Assert.assertEquals(2L, window.getIndexSequence());
    }

    @Test
    public void testGetSize() throws Exception {
        Assert.assertEquals(2, window.getSize());
    }

    @Test
    public void testSetSize() throws Exception {
        window.setSize(3);
        Assert.assertEquals(3, window.getSize());
    }

    @Test
    public void testGetStep() throws Exception {
        Assert.assertEquals(1, window.getStep());
    }

    @Test
    public void testSetStep() throws Exception {
        window.setStep(2);
        Assert.assertEquals(2, window.getStep());
    }

    @Test
    public void testGetTimeoutMillis() throws Exception {
        window.setHoleTimeoutMillis(50);
        Assert.assertEquals(50, window.getHoleTimeoutMillis());
    }

    @Test
    public void testSetTimeoutMillis() throws Exception {
        window.setHoleTimeoutMillis(100);
        Assert.assertEquals(100, window.getHoleTimeoutMillis());
    }

    @Test
    public void testReset() throws Exception {
        Assert.assertEquals(SlidingWindow.INITIAL_CONDITION, window.getIndexSequence());
        Assert.assertNull(window.getValueAtHighestSequence());
        Assert.assertNull(window.getValueAtLowestSequence());
    }

    @Test
    public void testReceive_DUPLICATE_SEQUENCE() throws Exception {

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(0, window.window.size());

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(0L, 0L, results));
        Assert.assertEquals(1, window.window.size());
        Assert.assertEquals(new Long(0), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(0), window.getValueAtHighestSequence());
        Assert.assertEquals(0L, window.getBeginningOfWindow());
        Assert.assertEquals(1L, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(0), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.DUPLICATE_SEQUENCE, window.receive(0L, 0L, results));
        Assert.assertEquals(1, window.window.size());
        Assert.assertEquals(new Long(0), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(0), window.getValueAtHighestSequence());
        Assert.assertEquals(0L, window.getBeginningOfWindow());
        Assert.assertEquals(1L, window.getEndOfWindow());
        Assert.assertEquals(0, results.size());
    }

    @Test
    public void testReceive_EXPECTED_SEQUENCE() throws Exception {

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(0, window.window.size());

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(0L, 0L, results));
        Assert.assertEquals(1, window.window.size());
        Assert.assertEquals(new Long(0), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(0), window.getValueAtHighestSequence());
        Assert.assertEquals(0L, window.getBeginningOfWindow());
        Assert.assertEquals(1L, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(0), results.get(0));
        results.clear();


        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1L, 1L, results));
        Assert.assertEquals(2, window.window.size());
        Assert.assertEquals(new Long(0), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(1), window.getValueAtHighestSequence());
        Assert.assertEquals(0L, window.getBeginningOfWindow());
        Assert.assertEquals(1L, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Thread.sleep(DEFAULT_MIN_EXPIRATION + 1);
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2L, 2L, results));
        Assert.assertEquals(2, window.window.size());
        Assert.assertEquals(new Long(1), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(2), window.getValueAtHighestSequence());
        Assert.assertEquals(1L, window.getBeginningOfWindow());
        Assert.assertEquals(2L, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(2), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(3L, 3L, results));
        Assert.assertEquals(2, window.window.size());
        Assert.assertEquals(new Long(2), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(3), window.getValueAtHighestSequence());
        Assert.assertEquals(2L, window.getBeginningOfWindow());
        Assert.assertEquals(3L, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(3), results.get(0));
        results.clear();

        Thread.sleep(DEFAULT_MIN_EXPIRATION + 1);
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(4L, 4L, results));
        Assert.assertEquals(2, window.window.size());
        Assert.assertEquals(new Long(3), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(4), window.getValueAtHighestSequence());
        Assert.assertEquals(3L, window.getBeginningOfWindow());
        Assert.assertEquals(4L, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(4), results.get(0));
    }

    @Test
    public void testReceive_HOLE_FILLED_Single() throws Exception {
        window.reset();
        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1L, 1L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2L, 2L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(2), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(4L, 4L, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(5L, 5L, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(7L, 7L, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.HOLE_FILLED, window.receive(6L, 6L, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.HOLE_FILLED, window.receive(3L, 3L, results));
        Assert.assertEquals(5, results.size());
        Assert.assertEquals(new Long(3), results.get(0));
        Assert.assertEquals(new Long(4), results.get(1));
        Assert.assertEquals(new Long(5), results.get(2));
        Assert.assertEquals(new Long(6), results.get(3));
        Assert.assertEquals(new Long(7), results.get(4));
    }

    @Test
    public void testReceive_HOLE_FILLED_Multiple() throws Exception {
        window.reset();
        window.setHoleTimeoutMillis(100000);
        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1L, 1L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2L, 2L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(2), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(4L, 4L, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(5L, 5L, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(7L, 7L, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.HOLE_FILLED, window.receive(3L, 3L, results));
        Assert.assertEquals(3, results.size());
        Assert.assertEquals(new Long(3), results.get(0));
        Assert.assertEquals(new Long(4), results.get(1));
        Assert.assertEquals(new Long(5), results.get(2));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.HOLE_FILLED, window.receive(6L, 6L, results));
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(new Long(6), results.get(0));
        Assert.assertEquals(new Long(7), results.get(1));
    }

    @Test
    public void testReceive_POSITIVE_HOLE() throws Exception {
        HoleTimeoutObserver holeTimeoutObserver = new HoleTimeoutObserver();
        PacketsReleasedObserver packetsReleasedObserver = new PacketsReleasedObserver();

        window.setHoleTimeoutMillis(10);
        window.setSize(4);
        window.onHoleTimeout(holeTimeoutObserver);
        window.onPacketsReleased(packetsReleasedObserver);

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1L, 1L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(4L, 4L, results));
        Assert.assertEquals(0, results.size());

        assertTrue(holeTimeoutObserver.latch.await(5, TimeUnit.SECONDS));
        assertTrue(packetsReleasedObserver.latch.await(5, TimeUnit.SECONDS));

        Assert.assertNotNull(holeTimeoutObserver.hole);
        Assert.assertEquals(2L, holeTimeoutObserver.hole.start);
        Assert.assertEquals(3L, holeTimeoutObserver.hole.end);

        Assert.assertNotNull(packetsReleasedObserver.packets);
        Assert.assertEquals(1, packetsReleasedObserver.packets.size());
        Assert.assertEquals(new Long(4), packetsReleasedObserver.packets.get(0));
    }

    @Test
    public void testLastValue() throws Exception {
        Long value = SlidingWindow.getNextValueAfter(Arrays.asList(0L, 1L, 2L, 3L, 4L).iterator(), 3);

        assertNotNull(value);
        assertEquals(4L, value);
    }

    @Test
    public void testLastValue2() throws Exception {
        Long value = SlidingWindow.getNextValueAfter(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L).iterator(), 3);

        assertNotNull(value);
        assertEquals(4L, value);
    }

    @Test
    public void testLastValue3() throws Exception {
        Long value = SlidingWindow.getNextValueAfter(Arrays.asList(3L, 4L, 5L).iterator(), 3);

        assertNotNull(value);
        assertEquals(4L, value);
    }

    @Test
    public void testLastValue4() throws Exception {
        Long value = SlidingWindow.getNextValueAfter(Arrays.asList(2L, 3L).iterator(), 3);

        assertNull(value);
    }

    @Test
    public void testLastValue5() throws Exception {
        Long value = SlidingWindow.getNextValueAfter(Arrays.asList(3L).iterator(), 3);

        assertNull(value);
    }

    @Test
    public void testLastValue6() throws Exception {
        Long value = SlidingWindow.getNextValueAfter(Arrays.asList(2L).iterator(), 3);

        assertNull(value);
    }

    @Test
    public void testLastValue7() throws Exception {
        Long value = SlidingWindow.getNextValueAfter(Arrays.asList(4L).iterator(), 3);

        assertEquals(4L, value);
    }

    //   [x, x, 0, 0, x, 0, 0, x, x]
    @Test
    public void testResultsBetween() throws Exception {
        window.indexSequence = 1L;

        window.holes.add(2L);
        window.holes.add(4L);

        window.window.put(1L, 1L);
        window.window.put(3L, 3L);
        window.window.put(6L, 6L);

        // returns [3]

        List<Long> results = window.getResultsAfterAndMoveIndex(2);

        assertNotNull(results);
        assertEquals(false, results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(3L, results.get(0));
    }

    @Test
    public void testResultsBetween2() throws Exception {
        window.indexSequence = 1L;

        window.holes.add(2L);

        window.window.put(1L, 1L);
        window.window.put(3L, 3L);
        window.window.put(4L, 4L);
        window.window.put(5L, 5L);
        window.window.put(6L, 6L);

        List<Long> results = window.getResultsAfterAndMoveIndex(2);

        assertNotNull(results);
        assertEquals(false, results.isEmpty());
        assertEquals(4, results.size());
        assertEquals(3L, results.get(0));
        assertEquals(4L, results.get(1));
        assertEquals(5L, results.get(2));
        assertEquals(6L, results.get(3));
    }

    @Test
    public void testResultsBetween3() throws Exception {
        window.indexSequence = 5L;

        window.holes.add(6L);

        window.window.put(1L, 1L);
        window.window.put(3L, 3L);
        window.window.put(4L, 4L);
        window.window.put(5L, 5L);

        List<Long> results = window.getResultsAfterAndMoveIndex(6);

        assertNotNull(results);
        assertEquals(true, results.isEmpty());
    }

    @Test
    public void testRangeBuilderNormal() throws Exception {
        Set<Long> holes = new TreeSet<Long>(Arrays.asList(0L, 1L, 2L));

        Set<SlidingWindow.HoleRange> ranges = window.buildHoleRanges(holes);

        assertEquals(1, ranges.size());
        SlidingWindow.HoleRange range = ranges.iterator().next();
        assertTrue(range.getKey().equals(key));
        assertTrue(range.getStart() == 0L);
        assertTrue(range.getEnd() == 2L);
    }

    @Test
    public void testRangeBuilderGap() throws Exception {
        Set<Long> holes = new TreeSet<Long>(Arrays.asList(0L, 2L));

        Set<SlidingWindow.HoleRange> ranges = window.buildHoleRanges(holes);

        assertEquals(2, ranges.size());

        Iterator<SlidingWindow.HoleRange> iterator = ranges.iterator();

        SlidingWindow.HoleRange range = iterator.next();
        assertTrue(range.getKey().equals(key));
        assertEquals(0L, range.getStart());
        assertEquals(0L, range.getEnd());

        range = iterator.next();
        assertTrue(range.getKey().equals(key));
        assertEquals(2L, range.getStart());
        assertEquals(2L, range.getEnd());
    }

    @Test
    public void testRangeBuilderGap2() throws Exception {
        Set<Long> holes = new TreeSet<Long>(Arrays.asList(0L, 1L, 3L, 4L));

        Set<SlidingWindow.HoleRange> ranges = window.buildHoleRanges(holes);

        assertEquals(2, ranges.size());

        Iterator<SlidingWindow.HoleRange> iterator = ranges.iterator();

        SlidingWindow.HoleRange range = iterator.next();
        assertTrue(range.getKey().equals(key));
        assertTrue(range.getStart() == 0L);
        assertTrue(range.getEnd() == 1L);

        range = iterator.next();
        assertTrue(range.getKey().equals(key));
        assertTrue(range.getStart() == 3L);
        assertTrue(range.getEnd() == 4L);
    }

    @Test
    public void testRangeBuilderGap3() throws Exception {
        Set<Long> holes = new TreeSet<Long>(Arrays.asList(0L, 1L, 3L, 4L, 6L));

        Set<SlidingWindow.HoleRange> ranges = window.buildHoleRanges(holes);

        assertEquals(3, ranges.size());

        Iterator<SlidingWindow.HoleRange> iterator = ranges.iterator();

        SlidingWindow.HoleRange range = iterator.next();
        assertTrue(range.getKey().equals(key));
        assertEquals(0L, range.getStart());
        assertEquals(1L, range.getEnd());

        range = iterator.next();
        assertEquals(key, range.getKey());
        assertEquals(3L, range.getStart());
        assertEquals(4L, range.getEnd());

        range = iterator.next();
        assertEquals(key, range.getKey());
        assertEquals(6L, range.getStart());
        assertEquals(6L, range.getEnd());
    }

    @Test
    public void testInit_POSITIVE_HOLE() throws Exception {

        window.setSize(100);
        window.setIndexSequence(301473);

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(301475L, 301475L, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(301476L, 301476L, results));
        Assert.assertEquals(0, results.size());

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(301477L, 301477L, results));
        Assert.assertEquals(0, results.size());

        Assert.assertEquals(SlidingWindow.ReceiveResult.HOLE_FILLED, window.receive(301474L, 301474L, results));
        Assert.assertEquals(4, results.size());
        Assert.assertEquals(new Long(301474L), results.get(0));
        Assert.assertEquals(new Long(301475L), results.get(1));
        Assert.assertEquals(new Long(301476L), results.get(2));
        Assert.assertEquals(new Long(301477L), results.get(3));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(301478L, 301478L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(301478L), results.get(0));
    }

    @Test
    public void testReceive_NEGATIVE_HOLE() throws Exception {

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(4L, 4L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(4), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(5L, 5L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(5), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(6L, 6L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(6), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.NEGATIVE_HOLE, window.receive(1L, 1L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        Assert.assertEquals(1L, window.getIndexSequence());
        Assert.assertEquals(1, window.window.size());
    }

    @Test
    public void testReceive_NEGATIVE_HOLE_InsideWindow() throws Exception {

        HoleTimeoutObserver holeTimeoutObserver = new HoleTimeoutObserver();
        PacketsReleasedObserver packetsReleasedObserver = new PacketsReleasedObserver();

        window.setSize(10);
        window.onHoleTimeout(holeTimeoutObserver);
        window.onPacketsReleased(packetsReleasedObserver);

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1L, 1L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2L, 2L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(2), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(5L, 5L, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Thread.sleep(2000); // Wait so that we will stop trying to fill the hole

        Assert.assertNotNull(holeTimeoutObserver.hole);
        Assert.assertEquals(3L, holeTimeoutObserver.hole.start);
        Assert.assertEquals(4L, holeTimeoutObserver.hole.end);

        Assert.assertNotNull(packetsReleasedObserver.packets);
        Assert.assertEquals(1, packetsReleasedObserver.packets.size());
        Assert.assertEquals(new Long(5), packetsReleasedObserver.packets.get(0));

        Assert.assertEquals(5L, window.getIndexSequence());

        // Even though this negative hole was inside the window it gets passed on even though it's now out of order
        Assert.assertEquals(SlidingWindow.ReceiveResult.NEGATIVE_HOLE, window.receive(3L, 3L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(3), results.get(0));
        Assert.assertEquals(5L, window.getIndexSequence());
    }

    @Test
    public void testReceive_NEGATIVE_HOLE_OutsideWindow() throws Exception {

        HoleTimeoutObserver holeTimeoutObserver = new HoleTimeoutObserver();
        PacketsReleasedObserver packetsReleasedObserver = new PacketsReleasedObserver();

        window.setSize(5);
        window.onHoleTimeout(holeTimeoutObserver);
        window.onPacketsReleased(packetsReleasedObserver);

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1L, 1L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(5L, 5L, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        assertTrue(holeTimeoutObserver.latch.await(4, TimeUnit.SECONDS));
        assertTrue(packetsReleasedObserver.latch.await(4, TimeUnit.SECONDS));

        Assert.assertNotNull(holeTimeoutObserver.hole);
        Assert.assertEquals(2L, holeTimeoutObserver.hole.start);
        Assert.assertEquals(4L, holeTimeoutObserver.hole.end);

        Assert.assertNotNull(packetsReleasedObserver.packets);
        Assert.assertEquals(1, packetsReleasedObserver.packets.size());
        Assert.assertEquals(new Long(5), packetsReleasedObserver.packets.get(0));

        Assert.assertEquals(5L, window.getIndexSequence());

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(6L, 6L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(6), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(7L, 7L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(7), results.get(0));
        results.clear();

        // Since this negative hole was outside the window it gets sent to us.
        Assert.assertEquals(SlidingWindow.ReceiveResult.NEGATIVE_HOLE, window.receive(2L, 2L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(2), results.get(0));
        Assert.assertEquals(2L, window.getIndexSequence());
    }

    @Test
    public void testReceive_NEGATIVE_HOLE_OnInit() throws Exception {

        List<Long> results = new ArrayList<Long>();
        window.setIndexSequence(200000L);

        Assert.assertEquals(SlidingWindow.ReceiveResult.NEGATIVE_HOLE, window.receive(1L, 1L, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(1L, window.getIndexSequence());
        Assert.assertEquals(1, window.window.size());
    }

    @Test
    public void testGetValueAtLowestSequence() throws Exception {

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(0L, 0L, results));
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1L, 1L, results));
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2L, 2L, results));
        Assert.assertEquals(new Long(0), window.getValueAtLowestSequence()); // Since we didn't expire the window the queue has grown to size 3
        Assert.assertEquals(3, window.window.size());
    }

    @Test
    public void testGetValueAtHighestSequence() throws Exception {

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(0L, 0L, results));
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1L, 1L, results));
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2L, 2L, results));
        Assert.assertEquals(new Long(2), window.getValueAtHighestSequence());
    }

    @Test
    public void testHasHoles() throws Exception {
        window.setSize(5);
        List<Long> results = new ArrayList<Long>();
        window.receive(2L, 2L, results);
        Assert.assertFalse(window.hasHoles());
        window.receive(1L, 1L, results);
        Assert.assertFalse(window.hasHoles());
        window.receive(4L, 4L, results);
        Assert.assertTrue(window.hasHoles());
        window.receive(3L, 3L, results);
        Assert.assertFalse(window.hasHoles());
    }

    @Test
    public void testFillsAHole() throws Exception {
        window.setSize(5);
        Assert.assertFalse(window.fillsAHole(0));
        Assert.assertFalse(window.fillsAHole(2));
        Assert.assertFalse(window.fillsAHole(3));
    }

    @Test
    public void testGetHoles() throws Exception {
        Set<Long> keys = new HashSet<Long>();
        Assert.assertEquals(0, window.getHoles(keys).size());

        keys.add(0L);
        Assert.assertEquals(0, window.getHoles(keys).size());

        keys.add(1L);
        Assert.assertEquals(0, window.getHoles(keys).size());

        keys.add(3L);
        List<SlidingWindow.HoleRange> holes = window.getHoles(keys);
        Assert.assertEquals(1, holes.size());
        Assert.assertEquals(2L, holes.get(0).start);
        Assert.assertEquals(2L, holes.get(0).end);

        keys.add(5L);
        holes = window.getHoles(keys);
        Assert.assertEquals(2, holes.size());

        Assert.assertEquals(2L, holes.get(0).start);
        Assert.assertEquals(2L, holes.get(0).end);

        Assert.assertEquals(4L, holes.get(1).start);
        Assert.assertEquals(4L, holes.get(1).end);
    }

    @Test
    public void testGetHolesAfterInit() throws Exception {
        window.setIndexSequence(2L);

        Set<Long> keys = new HashSet<Long>();
        Assert.assertEquals(0, window.getHoles(keys).size());

        keys.add(5L);
        List<SlidingWindow.HoleRange> holes = window.getHoles(keys);
        Assert.assertEquals(1, holes.size());
        Assert.assertEquals(3L, holes.get(0).start);
        Assert.assertEquals(4L, holes.get(0).end);
    }

    @Test
    public void testGetHolesAfterIntoAtZero() throws Exception {
        Set<Long> keys = new HashSet<Long>();
        Assert.assertEquals(0, window.getHoles(keys).size());

        keys.add(5L);
        List<SlidingWindow.HoleRange> holes = window.getHoles(keys);
        Assert.assertEquals(1, holes.size());
        Assert.assertEquals(0L, holes.get(0).start);
        Assert.assertEquals(4L, holes.get(0).end);
    }

    private class HoleTimeoutObserver implements Observer<SlidingWindow.HoleRange> {

        protected SlidingWindow.HoleRange hole;
        public CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void notify(Object sender, SlidingWindow.HoleRange item) {
            hole = item;
            latch.countDown();
        }
    }

    private class PacketsReleasedObserver implements Observer<List<Long>> {

        protected List<Long> packets;
        public CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void notify(Object sender, List<Long> items) {
            packets = items;
            latch.countDown();
        }
    }

}

