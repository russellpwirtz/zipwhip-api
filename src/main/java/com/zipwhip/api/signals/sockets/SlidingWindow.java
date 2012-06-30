package com.zipwhip.api.signals.sockets;

import com.zipwhip.events.*;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.DestroyableBase;
import org.jboss.netty.util.*;
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

    protected static final int DEFAULT_WINDOW_SIZE = 10;

    // Backing data structure holding an ordered map
    protected final FlexibleEvictingQueue<Long, P> window = new FlexibleEvictingQueue<Long, P>();

    // This is used to fire notifications if a hole was not filled inside the timeout window
    private final ObservableHelper<HoleRange> holeTimeoutEvent = new ObservableHelper<HoleRange>();

    // This timer schedules our hole timeout waits
    private final HashedWheelTimer timer = new HashedWheelTimer();

    // The last known sequence that was released from the window
    private long indexSequence = 0L;

    // A way to identify the channel we have a window on
    private String key;

    // The ideal size of the window. The window can grow larger within a given time period.
    private int size = DEFAULT_WINDOW_SIZE;

    // The default step between sequence numbers
    private int step = 1;

    // How long to wait for holes to fill in
    private int timeoutMillis = 500;

    /**
     * Construct a SlidingWindow with a default window size.
     */
    public SlidingWindow(String key) {
        this(key, DEFAULT_WINDOW_SIZE);
    }

    /**
     * Construct a SlidingWindow,
     *
     * @param size The size of the sliding window.
     */
    public SlidingWindow(String key, int size) {
        this.key = key;
        this.size = size;
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
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public void onHoleTimeout(Observer<HoleRange> observable) {
        holeTimeoutEvent.addObserver(observable);
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

        // EXPECTED_SEQUENCE
        if (indexSequence <= 0 || (sequence == indexSequence + step && !hasHoles(window.keySet()))) {

            // Add a single result
            results.add(value);

            indexSequence = sequence;
            window.put(sequence, value);

            // Slide the window if it is full
            if (window.size() > size) {
                window.pollFirstEntry();
            }

            return ReceiveResult.EXPECTED_SEQUENCE;
        }

        // HOLE_FILLED
        if (hasHoles(window.keySet()) && fillsAHole(sequence)) {

            window.put(sequence, value);

            // We only want to add the results if the filled hole was the first hole
            if (!hasHoles(window.headMap(sequence).keySet())) {

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

                for (P p : results) {
                    // Slide the window if it is full
                    if (window.size() > size) {
                        window.pollFirstEntry();
                    } else {
                        break;
                    }
                }
            }
            return ReceiveResult.HOLE_FILLED;
        }

        // POSITIVE_HOLE
        if (sequence > indexSequence + step) {

            window.put(sequence, value);

            List<HoleRange> holes = getHoles(window.keySet());

            for (HoleRange hole : holes) {
                if (hole.endInclusive == sequence) {
                    waitForHole(hole);
                }
            }

            // No results to release since we just created a hole
            return ReceiveResult.POSITIVE_HOLE;
        }

        // NEGATIVE_HOLE
        if (sequence < indexSequence + step) {

            // Add a single result
            results.add(value);

            indexSequence = sequence;
            window.clear(); // TODO Michael claims that this is not necessary but I don't see it yet...
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
                        // TODO 1. Set another event and wait 2x for the hole to be back filled.
                        // TODO 2. If it is not filled at that timeout slide the window to [endInclusive + step].
                        // TODO 3.
                    }
                }
            }
        };

        timer.newTimeout(waitAndNotifyTask, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * The inclusive indexes of the window are defined as [window.firstKey(), window.firstKey() + size -1]
     */
    protected long getBeginningOfWindow() {
        return window.firstKey();
    }

    protected long getEndOfWindow() {
        return window.firstKey() + size - 1;
    }

    /**
     * Are there any holes in the current window?
     *
     * @return true if holes exist, otherwise false
     */
    protected boolean hasHoles(Set<Long> keys) {
        long previous = -1;
        for (Long sequence : keys) {
            if (previous != -1 && previous + step != sequence) {
                return true;
            }
            previous = sequence;
        }
        return false;
    }

    protected boolean fillsAHole(long sequence) {
        Set<Long> keysBefore = new HashSet<Long>(window.keySet());
        Set<Long> keysAfter = new HashSet<Long>(window.keySet());
        keysAfter.add(sequence);

        return getHoles(keysBefore).size() > getHoles(keysAfter).size();
    }

    protected List<HoleRange> getHoles(Set<Long> keys) {

        List<HoleRange> holes = new ArrayList<HoleRange>();

        long previous = -1;

        for (Long sequence : keys) {
            if (previous != -1 && previous + step != sequence) {
                HoleRange range = new HoleRange(key, previous, sequence);
                holes.add(range);
            }
            previous = sequence;
        }
        return holes;
    }

    protected static class HoleRange {

        protected String key;
        protected long startExclusive;
        protected long endInclusive;

        public HoleRange(String key, long startExclusive, long endInclusive) {
            this.key = key;
            this.startExclusive = startExclusive;
            this.endInclusive = endInclusive;
        }

        public List<Long> getRange() {
            List<Long> range = new ArrayList<Long>();
            range.add(startExclusive);
            range.add(endInclusive);
            return range;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof HoleRange)) return false;
            HoleRange o = (HoleRange) obj;
            return o.startExclusive == startExclusive && o.endInclusive == endInclusive;
        }

        @Override
        public String toString() {
            return "[" + startExclusive + "," + endInclusive + "]";
        }
    }

    @Override
    protected void onDestroy() {
        timer.stop();
        holeTimeoutEvent.destroy();
    }

}
