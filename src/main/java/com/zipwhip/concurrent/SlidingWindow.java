package com.zipwhip.concurrent;

import com.zipwhip.executors.NamedThreadFactory;
import com.zipwhip.events.*;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.FlexibleTimedEvictionMap;
import org.jboss.netty.util.*;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 6/25/12
 * Time: 12:40 PM
 */
public class SlidingWindow<P> extends DestroyableBase {

    public enum ReceiveResult {
        EXPECTED_SEQUENCE,
        DUPLICATE_SEQUENCE,
        POSITIVE_HOLE,
        NEGATIVE_HOLE,
        HOLE_FILLED,
        UNKNOWN_RESULT
    }

    protected static final int DEFAULT_WINDOW_SIZE = 100;
    protected static final long DEFAULT_MINIMUM_EVICTION_AGE = 5 * 60 * 1000;

    // Backing data structure holding an ordered map
    protected FlexibleTimedEvictionMap<Long, P> window;

    // This is used to fire notifications if a hole was not filled inside the timeout window
    private final ObservableHelper<HoleRange> holeTimeoutEvent = new ObservableHelper<HoleRange>();

    // This is used to fire notifications that a timeout moved the window and released some packets
    private final ObservableHelper<List<P>> packetsReleasedEvent = new ObservableHelper<List<P>>();

    // This timer schedules our hole timeout waits
    private final Timer timer;

    // The last known sequence that was released from the window
    private long indexSequence = 0L;

    // A way to identify the channel we have a window on
    private String key;

    // The default step between sequence numbers
    private int step = 1;

    // How long to wait for holes to fill in
    private int holeTimeoutMillis = 500;

    /**
     * Construct a SlidingWindow with a default window size and eviction time.
     */
    public SlidingWindow(Timer timer, String key) {
        this(timer, key, DEFAULT_WINDOW_SIZE, DEFAULT_MINIMUM_EVICTION_AGE);
    }

    /**
     * Construct a SlidingWindow,
     *
     * @param idealSize                 The ideal size of the sliding window.
     * @param minimumEvictionTimeMillis The time in milliseconds that a packet will be kept in the window.
     */
    public SlidingWindow(Timer timer, String key, int idealSize, long minimumEvictionTimeMillis) {
        if (timer == null){
            timer = new HashedWheelTimer(new NamedThreadFactory("SlidingWindow-"));
        }
        this.timer = timer;
        this.key = key;
        this.window = new FlexibleTimedEvictionMap<Long, P>(idealSize, minimumEvictionTimeMillis);
    }

    public long getIndexSequence() {
        return indexSequence;
    }

    public void setIndexSequence(long indexSequence) {
        this.indexSequence = indexSequence;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getSize() {
        return window.getIdealSize();
    }

    public void setSize(int size) {
        window.setIdealSize(size);
    }

    public long getMinimumEvictionAgeMillis() {
        return window.getMinimumEvictionAgeMillis();
    }

    public void setMinimumEvictionAgeMillis(long minimumEvictionAgeMillis) {
        window.setMinimumEvictionAgeMillis(minimumEvictionAgeMillis);
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public int getHoleTimeoutMillis() {
        return holeTimeoutMillis;
    }

    public void setHoleTimeoutMillis(int holeTimeoutMillis) {
        this.holeTimeoutMillis = holeTimeoutMillis;
    }

    public void onHoleTimeout(Observer<HoleRange> observable) {
        holeTimeoutEvent.addObserver(observable);
    }

    public void onPacketsReleased(Observer<List<P>> observable) {
        packetsReleasedEvent.addObserver(observable);
    }

    /**
     * Tell the window that a discrete packet of type P has been received.
     * Returns a ReceiveResult enum indicating the state of the window after the sequence was received.
     * <p/>
     * EXPECTED_SEQUENCE - Sequence received was equal to last received {@code indexSequence} + {@code step}.
     * Post-condition: If the window is full the window slides. Received sequence becomes the indexSequence.
     * </p>
     * DUPLICATE_SEQUENCE - Sequence received is already inside the window.
     * Post-condition: Noop.
     * </p>
     * POSITIVE_HOLE - Sequence received is ahead of {@code indexSequence} + {@code step}.
     * Post-condition: A timer is set to wait for the hole to fill. No packets are released.
     * </p>
     * NEGATIVE_HOLE - Sequence received was behind {@code indexSequence} + {@code step}.
     * Post-condition: The window is reset, this sequence is set to {@code indexSequence} and this packet is released.
     * </p>
     * HOLE_FILLED - Sequence received filled a hole in the window.
     * Post-condition: The packets inside the hole are released in order and last sequence released becomes the {@code indexSequence}.
     * </p>
     * UNKNOWN_RESULT - Should never happen, can be used as the default entry for switching.
     * Post-condition: Noop.
     *
     * @param sequence The sequence number.
     * @param value    The value at this sequence number.
     * @param results  A list to add the results to if any packets should be released.
     * @return An enum ReceiveResult indicating the state of the window after the sequence was received.
     */
    public ReceiveResult receive(Long sequence, P value, List<P> results) {

        if (results == null) {
            results = new ArrayList<P>();
        }

        // DUPLICATE_SEQUENCE
        if (window.containsKey(sequence) && window.get(sequence).equals(value)) {
            // No results to release, this packet gets dropped
            return ReceiveResult.DUPLICATE_SEQUENCE;
        }

        // HOLE_FILLED
        if (hasHoles(window.keySet()) && fillsAHole(sequence)) {

            window.put(sequence, value);

            // We only want to add the results if the filled hole was the first hole
            if (!hasHoles(window.headMap(sequence).keySet())) {
                results.addAll(getResultsFromIndexSequenceForward(sequence));
            }
            return ReceiveResult.HOLE_FILLED;
        }

        // EXPECTED_SEQUENCE
        if (indexSequence <= 0 || sequence.equals(indexSequence + step)) {

            // Add a single result
            results.add(value);

            indexSequence = sequence;
            window.put(sequence, value);
            window.shrink();

            return ReceiveResult.EXPECTED_SEQUENCE;
        }

        // POSITIVE_HOLE
        if (sequence > indexSequence + step) {

            window.put(sequence, value);

            List<HoleRange> holes = getHoles(window.keySet());

            for (HoleRange hole : holes) {
                if (sequence.equals(hole.end + step)) {
                    waitForHole(hole);
                }
            }

            // No results to release since we just created a hole
            return ReceiveResult.POSITIVE_HOLE;
        }

        // NEGATIVE_HOLE
        if (sequence < indexSequence + step) {

            // This sequence is much lower, must be a reset
            if (indexSequence + step - sequence > window.getIdealSize()) {
                indexSequence = sequence;
                window.clear();
            }

            // Always pass this packet through even if it's out of order.
            results.add(value);
            window.put(sequence, value);

            return ReceiveResult.NEGATIVE_HOLE;
        }

        // If we got here something is wrong
        return ReceiveResult.UNKNOWN_RESULT;
    }

    /**
     * Resets the indexSequence to an invalid sequence number and clears the stored data in the window.
     */
    public void reset() {
        indexSequence = -1L;
        window.clear();
    }

    /**
     * Get item with the lowest sequence number inside the window.
     *
     * @return The item with the lowest sequence number inside the window.
     */
    public P getValueAtLowestSequence() {
        try {
            return window.get(window.firstKey());
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Get item with the highest sequence number inside the window.
     *
     * @return The item with the highest sequence number inside the window.
     */
    public P getValueAtHighestSequence() {
        try {
            return window.get(window.lastKey());
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    protected void waitForHole(final HoleRange hole) {

        TimerTask waitAndNotifyTask = new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {

                for (HoleRange h : getHoles(window.keySet())) {

                    if (hole.equals(h)) {

                        holeTimeoutEvent.notifyObservers(this, hole);

                        // Set another event and wait 2x for the hole to be back filled.
                        TimerTask waitAndCheckTask = new TimerTask() {
                            @Override
                            public void run(Timeout timeout) throws Exception {

                                for (HoleRange h : getHoles(window.keySet())) {

                                    if (hole.equals(h)) {
                                        // If it is not filled at this timeout then slide the window to end.
                                        indexSequence = hole.end;
                                        List<P> results = getResultsFromIndexSequenceForward(indexSequence);

                                        if (!results.isEmpty()) {
                                            packetsReleasedEvent.notifyObservers(this, results);
                                        }
                                    }
                                }
                            }
                        };
                        timer.newTimeout(waitAndCheckTask, holeTimeoutMillis * 2, TimeUnit.MILLISECONDS);
                    }
                }
            }
        };
        timer.newTimeout(waitAndNotifyTask, holeTimeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * The inclusive indexes of the window are defined as [window.firstKey(), window.firstKey() + size -1]
     */
    protected long getBeginningOfWindow() {
        return window.firstKey();
    }

    protected long getEndOfWindow() {
        return window.firstKey() + window.getIdealSize() - 1;
    }

    /**
     * Are there any holes in the current window?
     *
     * @return true if holes exist, otherwise false
     */
    protected boolean hasHoles(Set<Long> keys) {

        long previous = indexSequence;

        for (Long sequence : keys) {
            if (sequence > indexSequence && previous + step != sequence) {
                return true;
            }
            previous = sequence;
        }
        return false;
    }

    protected boolean fillsAHole(long sequence) {
        Set<Long> keysBefore = new TreeSet<Long>(window.keySet());
        Set<Long> keysAfter = new TreeSet<Long>(window.keySet());
        keysAfter.add(sequence);

        return getHoles(keysBefore).size() > getHoles(keysAfter).size();
    }

    protected List<HoleRange> getHoles(Set<Long> keys) {

        List<HoleRange> holes = new ArrayList<HoleRange>();

        long previous = indexSequence;

        for (Long sequence : keys) {
            if (previous >= 0 && previous + step != sequence) {
                HoleRange range = new HoleRange(key, previous + step, sequence - step);
                holes.add(range);
            }
            previous = sequence;
        }
        return holes;
    }

    /**
     * Get the results from the {@code sequence} forward up to the end of the list
     * unless a hole is found going forward. If a hole is found then the results are
     * {@code sequence} + {@code step} to the next hole.
     *
     * @param sequence The starting sequence to find results from.
     * @return A list of the results or an empty list if there are non.
     */
    protected List<P> getResultsFromIndexSequenceForward(long sequence) {

        List<P> results = new ArrayList<P>();

        NavigableMap<Long, P> tail = window.tailMap(sequence, true);
        long previous = sequence;

        for (Long key : tail.keySet()) {
            if (key != previous && key != previous + step) {
                // This case indicates that we found a hole higher in the sequence
                break;
            }
            results.add(tail.get(key));
            previous = key;
            indexSequence = key;
        }

        // Attempt to resize the map
        window.shrink(results.size());

        return results;
    }

    public static class HoleRange {

        protected String key;
        protected long start;
        protected long end;

        public HoleRange(String key, long start, long end) {
            this.key = key;
            this.start = start;
            this.end = end;
        }

        public List<Long> getRange() {
            List<Long> range = new ArrayList<Long>();
            range.add(start);
            range.add(end);
            return range;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof HoleRange)) return false;
            HoleRange o = (HoleRange) obj;
            return o.start == start && o.end == end;
        }

        public String getKey() {
            return key;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return "[" + start + "," + end + "]";
        }
    }

    @Override
    protected void onDestroy() {
        timer.stop();
        holeTimeoutEvent.destroy();
    }

}
