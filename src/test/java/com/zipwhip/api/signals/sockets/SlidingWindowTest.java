package com.zipwhip.api.signals.sockets;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 6/25/12
 * Time: 4:43 PM
 */
public class SlidingWindowTest {

    SlidingWindow<Long> window;
    String key = "channel:/1234-5678-9012";

    @Before
    public void setUp() throws Exception {
        window = new SlidingWindow<Long>(key);
        window.setSize(2);
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
        Assert.assertEquals(0L, window.getIndexSequence());
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
        window.setTimeoutMillis(50);
        Assert.assertEquals(50, window.getTimeoutMillis());
    }

    @Test
    public void testSetTimeoutMillis() throws Exception {
        window.setTimeoutMillis(100);
        Assert.assertEquals(100, window.getTimeoutMillis());
    }

    @Test
    public void testReset() throws Exception {
        Assert.assertEquals(0L, window.getIndexSequence());
        Assert.assertNull(window.getValueAtHighestSequence());
        Assert.assertNull(window.getValueAtLowestSequence());
    }

    @Test
    public void testReceive_DUPLICATE_SEQUENCE() throws Exception {

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(0, window.window.size());

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(0l, 0l, results));
        Assert.assertEquals(1, window.window.size());
        Assert.assertEquals(new Long(0), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(0), window.getValueAtHighestSequence());
        Assert.assertEquals(0l, window.getBeginningOfWindow());
        Assert.assertEquals(1l, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(0), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.DUPLICATE_SEQUENCE, window.receive(0l, 0l, results));
        Assert.assertEquals(1, window.window.size());
        Assert.assertEquals(new Long(0), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(0), window.getValueAtHighestSequence());
        Assert.assertEquals(0l, window.getBeginningOfWindow());
        Assert.assertEquals(1l, window.getEndOfWindow());
        Assert.assertEquals(0, results.size());
    }

    @Test
    public void testReceive_EXPECTED_SEQUENCE() throws Exception {

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(0, window.window.size());

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(0l, 0l, results));
        Assert.assertEquals(1, window.window.size());
        Assert.assertEquals(new Long(0), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(0), window.getValueAtHighestSequence());
        Assert.assertEquals(0l, window.getBeginningOfWindow());
        Assert.assertEquals(1l, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(0), results.get(0));
        results.clear();


        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1l, 1l, results));
        Assert.assertEquals(2, window.window.size());
        Assert.assertEquals(new Long(0), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(1), window.getValueAtHighestSequence());
        Assert.assertEquals(0l, window.getBeginningOfWindow());
        Assert.assertEquals(1l, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2l, 2l, results));
        Assert.assertEquals(2, window.window.size());
        Assert.assertEquals(new Long(1), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(2), window.getValueAtHighestSequence());
        Assert.assertEquals(1l, window.getBeginningOfWindow());
        Assert.assertEquals(2l, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(2), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(3l, 3l, results));
        Assert.assertEquals(2, window.window.size());
        Assert.assertEquals(new Long(2), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(3), window.getValueAtHighestSequence());
        Assert.assertEquals(2l, window.getBeginningOfWindow());
        Assert.assertEquals(3l, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(3), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(4l, 4l, results));
        Assert.assertEquals(2, window.window.size());
        Assert.assertEquals(new Long(3), window.getValueAtLowestSequence());
        Assert.assertEquals(new Long(4), window.getValueAtHighestSequence());
        Assert.assertEquals(3l, window.getBeginningOfWindow());
        Assert.assertEquals(4l, window.getEndOfWindow());
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(4), results.get(0));
    }

    @Test
    public void testReceive_HOLE_FILLED_Single() throws Exception {

        window.reset();
        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1l, 1l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2l, 2l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(2), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(4l, 4l, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(5l, 5l, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(7l, 7l, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.HOLE_FILLED, window.receive(6l, 6l, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.HOLE_FILLED, window.receive(3l, 3l, results));
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
        window.setTimeoutMillis(10);
        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1l, 1l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2l, 2l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(2), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(4l, 4l, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(5l, 5l, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(7l, 7l, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.HOLE_FILLED, window.receive(3l, 3l, results));
        Assert.assertEquals(3, results.size());
        Assert.assertEquals(new Long(3), results.get(0));
        Assert.assertEquals(new Long(4), results.get(1));
        Assert.assertEquals(new Long(5), results.get(2));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.HOLE_FILLED, window.receive(6l, 6l, results));
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(new Long(6), results.get(0));
        Assert.assertEquals(new Long(7), results.get(1));
    }

    @Test
    public void testReceive_POSITIVE_HOLE() throws Exception {
        window.setSize(4);
        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1l, 1l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(4l, 4l, results));
        Assert.assertEquals(0, results.size());
    }

    @Test
    public void testReceive_NEGATIVE_HOLE() throws Exception {

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1l, 1l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2l, 2l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(2), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(3l, 3l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(3), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.NEGATIVE_HOLE, window.receive(1l, 1l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(1l, window.getIndexSequence());
        Assert.assertEquals(1, window.window.size());
        Assert.assertEquals(1, window.window.size());
    }

    @Test
    public void testGetValueAtLowestSequence() throws Exception {

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(0l, 0l, results));
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1l, 1l, results));
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2l, 2l, results));
        Assert.assertEquals(new Long(1), window.getValueAtLowestSequence());
    }

    @Test
    public void testGetValueAtHighestSequence() throws Exception {

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(0l, 0l, results));
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1l, 1l, results));
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2l, 2l, results));
        Assert.assertEquals(new Long(2), window.getValueAtHighestSequence());
    }

    @Test
    public void testHasHoles() throws Exception {
        window.setSize(5);
        List<Long> results = new ArrayList<Long>();
        window.receive(0l, 0l, results);
        Assert.assertFalse(window.hasHoles(window.window.keySet()));
        window.receive(2l, 2l, results);
        Assert.assertTrue(window.hasHoles(window.window.keySet()));
        window.receive(1l, 1l, results);
        Assert.assertFalse(window.hasHoles(window.window.keySet()));
        window.receive(4l, 4l, results);
        Assert.assertTrue(window.hasHoles(window.window.keySet()));
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

        keys.add(1l);
        Assert.assertEquals(0, window.getHoles(keys).size());

        keys.add(2l);
        Assert.assertEquals(0, window.getHoles(keys).size());

        keys.add(4l);
        List<SlidingWindow.HoleRange> holes = window.getHoles(keys);
        Assert.assertEquals(1, holes.size());
        Assert.assertEquals(2L, holes.get(0).startExclusive);
        Assert.assertEquals(4L, holes.get(0).endInclusive);

        keys.add(6l);
        holes = window.getHoles(keys);
        Assert.assertEquals(2, holes.size());

        Assert.assertEquals(2L, holes.get(0).startExclusive);
        Assert.assertEquals(4L, holes.get(0).endInclusive);

        Assert.assertEquals(4L, holes.get(1).startExclusive);
        Assert.assertEquals(6L, holes.get(1).endInclusive);
    }

}

