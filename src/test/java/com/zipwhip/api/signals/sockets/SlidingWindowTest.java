package com.zipwhip.api.signals.sockets;

import com.zipwhip.events.Observer;
import junit.framework.Assert;
import org.apache.log4j.BasicConfigurator;
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

    static final int DEFAULT_WINDOW_SIZE = 2;
    static final long DEFAULT_MIN_EXPIRATION = 100;

    @Before
    public void setUp() throws Exception {
        window = new SlidingWindow<Long>(key, DEFAULT_WINDOW_SIZE, DEFAULT_MIN_EXPIRATION);
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

        Thread.sleep(DEFAULT_MIN_EXPIRATION + 1);
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

        Thread.sleep(DEFAULT_MIN_EXPIRATION + 1);
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
        window.setHoleTimeoutMillis(10);
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

        HoleTimeoutObserver holeTimeoutObserver = new HoleTimeoutObserver();
        PacketsReleasedObserver packetsReleasedObserver = new PacketsReleasedObserver();

        window.setSize(4);
        window.onHoleTimeout(holeTimeoutObserver);
        window.onPacketsReleased(packetsReleasedObserver);

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1l, 1l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(4l, 4l, results));
        Assert.assertEquals(0, results.size());

        Thread.sleep(2000); // Wait so that we will stop trying to fill the hole

        Assert.assertNotNull(holeTimeoutObserver.hole);
        Assert.assertEquals(2l, holeTimeoutObserver.hole.start);
        Assert.assertEquals(3l, holeTimeoutObserver.hole.end);

        Assert.assertNotNull(packetsReleasedObserver.packets);
        Assert.assertEquals(1, packetsReleasedObserver.packets.size());
        Assert.assertEquals(new Long(4), packetsReleasedObserver.packets.get(0));
    }

    @Test
    public void testInit_POSITIVE_HOLE() throws Exception {

        window.setSize(100);
        window.setIndexSequence(301473);

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(301475l, 301475l, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(301476l, 301476l, results));
        Assert.assertEquals(0, results.size());

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(301477l, 301477l, results));
        Assert.assertEquals(0, results.size());

        Assert.assertEquals(SlidingWindow.ReceiveResult.HOLE_FILLED, window.receive(301474l, 301474l, results));
        Assert.assertEquals(4, results.size());
        Assert.assertEquals(new Long(301474l), results.get(0));
        Assert.assertEquals(new Long(301475l), results.get(1));
        Assert.assertEquals(new Long(301476l), results.get(2));
        Assert.assertEquals(new Long(301477l), results.get(3));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(301478l, 301478l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(301478l), results.get(0));
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

        Assert.assertEquals(SlidingWindow.ReceiveResult.NEGATIVE_HOLE, window.receive(1l, 10l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(10), results.get(0));
        Assert.assertEquals(1l, window.getIndexSequence());
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

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1l, 1l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2l, 2l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(2), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(5l, 5l, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Thread.sleep(2000); // Wait so that we will stop trying to fill the hole

        Assert.assertNotNull(holeTimeoutObserver.hole);
        Assert.assertEquals(3l, holeTimeoutObserver.hole.start);
        Assert.assertEquals(4l, holeTimeoutObserver.hole.end);

        Assert.assertNotNull(packetsReleasedObserver.packets);
        Assert.assertEquals(1, packetsReleasedObserver.packets.size());
        Assert.assertEquals(new Long(5), packetsReleasedObserver.packets.get(0));

        Assert.assertEquals(5l, window.getIndexSequence());

        // Even though this negative hole was inside the window it gets passed on even though it's now out of order
        Assert.assertEquals(SlidingWindow.ReceiveResult.NEGATIVE_HOLE, window.receive(3l, 3l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(3), results.get(0));
        Assert.assertEquals(5l, window.getIndexSequence());
    }

    @Test
    public void testReceive_NEGATIVE_HOLE_OutsideWindow() throws Exception {

        HoleTimeoutObserver holeTimeoutObserver = new HoleTimeoutObserver();
        PacketsReleasedObserver packetsReleasedObserver = new PacketsReleasedObserver();

        window.setSize(5);
        window.onHoleTimeout(holeTimeoutObserver);
        window.onPacketsReleased(packetsReleasedObserver);

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1l, 1l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.POSITIVE_HOLE, window.receive(5l, 5l, results));
        Assert.assertEquals(0, results.size());
        results.clear();

        Thread.sleep(2000); // Wait so that we will stop trying to fill the hole

        Assert.assertNotNull(holeTimeoutObserver.hole);
        Assert.assertEquals(2l, holeTimeoutObserver.hole.start);
        Assert.assertEquals(4l, holeTimeoutObserver.hole.end);

        Assert.assertNotNull(packetsReleasedObserver.packets);
        Assert.assertEquals(1, packetsReleasedObserver.packets.size());
        Assert.assertEquals(new Long(5), packetsReleasedObserver.packets.get(0));

        Assert.assertEquals(5l, window.getIndexSequence());

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(6l, 6l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(6), results.get(0));
        results.clear();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(7l, 7l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(7), results.get(0));
        results.clear();

        // Since this negative hole was outside the window it gets sent to us.
        Assert.assertEquals(SlidingWindow.ReceiveResult.NEGATIVE_HOLE, window.receive(2l, 2l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(2), results.get(0));
        Assert.assertEquals(2l, window.getIndexSequence());
    }

    @Test
    public void testReceive_NEGATIVE_HOLE_OnInit() throws Exception {

        BasicConfigurator.configure();

        List<Long> results = new ArrayList<Long>();
        window.setIndexSequence(200000l);

        Assert.assertEquals(SlidingWindow.ReceiveResult.NEGATIVE_HOLE, window.receive(1l, 1l, results));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(new Long(1), results.get(0));
        results.clear();

        Assert.assertEquals(1l, window.getIndexSequence());
        Assert.assertEquals(1, window.window.size());
    }

    @Test
    public void testGetValueAtLowestSequence() throws Exception {

        List<Long> results = new ArrayList<Long>();

        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(0l, 0l, results));
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(1l, 1l, results));
        Assert.assertEquals(SlidingWindow.ReceiveResult.EXPECTED_SEQUENCE, window.receive(2l, 2l, results));
        Assert.assertEquals(new Long(0), window.getValueAtLowestSequence()); // Since we didn't expire the window the queue has grown to size 3
        Assert.assertEquals(3, window.window.size());
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
        window.receive(2l, 2l, results);
        Assert.assertFalse(window.hasHoles(window.window.keySet()));
        window.receive(1l, 1l, results);
        Assert.assertFalse(window.hasHoles(window.window.keySet()));
        window.receive(4l, 4l, results);
        Assert.assertTrue(window.hasHoles(window.window.keySet()));
        window.receive(3l, 3l, results);
        Assert.assertFalse(window.hasHoles(window.window.keySet()));
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
        Assert.assertEquals(3L, holes.get(0).start);
        Assert.assertEquals(3L, holes.get(0).end);

        keys.add(6l);
        holes = window.getHoles(keys);
        Assert.assertEquals(2, holes.size());

        Assert.assertEquals(3L, holes.get(0).start);
        Assert.assertEquals(3L, holes.get(0).end);

        Assert.assertEquals(5L, holes.get(1).start);
        Assert.assertEquals(5L, holes.get(1).end);
    }

    @Test
    public void testGetHolesAfterInit() throws Exception {

        window.setIndexSequence(2L);

        Set<Long> keys = new HashSet<Long>();
        Assert.assertEquals(0, window.getHoles(keys).size());


        keys.add(5l);
        List<SlidingWindow.HoleRange> holes = window.getHoles(keys);
        Assert.assertEquals(1, holes.size());
        Assert.assertEquals(3L, holes.get(0).start);
        Assert.assertEquals(4L, holes.get(0).end);
    }

    @Test
    public void testGetHolesAfterIntoAtZero() throws Exception {

        Set<Long> keys = new HashSet<Long>();
        Assert.assertEquals(0, window.getHoles(keys).size());

        keys.add(5l);
        List<SlidingWindow.HoleRange> holes = window.getHoles(keys);
        Assert.assertEquals(1, holes.size());
        Assert.assertEquals(1L, holes.get(0).start);
        Assert.assertEquals(4L, holes.get(0).end);
    }

    private class HoleTimeoutObserver implements Observer<SlidingWindow.HoleRange> {

        protected SlidingWindow.HoleRange hole;

        @Override
        public void notify(Object sender, SlidingWindow.HoleRange item) {
            hole = item;
        }
    }

    private class PacketsReleasedObserver implements Observer<List<Long>> {

        protected List<Long> packets;

        @Override
        public void notify(Object sender, List<Long> items) {
            packets = items;
        }
    }

}

