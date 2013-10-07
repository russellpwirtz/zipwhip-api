package com.zipwhip.concurrent;

import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.NamedThreadFactory;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.timers.HashedWheelTimer;
import com.zipwhip.timers.Timeout;
import com.zipwhip.timers.Timer;
import com.zipwhip.timers.TimerTask;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.FlexibleTimedEvictionMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 6/25/12
 * Time: 12:40 PM
 */
public class SlidingWindow<P> extends DestroyableBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlidingWindow.class);

    protected static final int INITIAL_CONDITION = -1;

    private static final Comparator<? super HoleRange> HOLE_RANGE_COMPARATOR = new Comparator<HoleRange>() {
        @Override
        public int compare(HoleRange o1, HoleRange o2) {
            if (o1.start > o2.start) {
                return 1;
            } else if (o1.start == o2.start) {
                return 0;
            } else {
                return -1;
            }
        }
    };

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
    protected Set<Long> holes;

    // This is used to fire notifications if a hole was not filled inside the timeout window
    private final ObservableHelper<HoleRange> holeTimeoutEvent = new ObservableHelper<HoleRange>();

    // This is used to fire notifications that a timeout moved the window and released some packets
    private final ObservableHelper<List<P>> packetsReleasedEvent = new ObservableHelper<List<P>>();

    // This timer schedules our hole timeout waits
    private final Timer timer;

    // The last known sequence that was released from the window
    protected long indexSequence = INITIAL_CONDITION;

    // A way to identify the channel we have a window on
    private String key;

    // The default step between sequence numbers
    private int step = 1;

    // How long to wait for holes to fill in
    private int holeTimeoutMillis = 5000;

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
        if (timer == null) {
            timer = new HashedWheelTimer(new NamedThreadFactory("SlidingWindow-"));
        }
        this.timer = timer;
        this.key = key;
        this.window = new FlexibleTimedEvictionMap<Long, P>(idealSize, minimumEvictionTimeMillis);
        this.holes = new TreeSet<Long>();
    }

    public long getIndexSequence() {
        return indexSequence;
    }

    public void setIndexSequence(long indexSequence) {
        if (indexSequence < INITIAL_CONDITION) {
            indexSequence = INITIAL_CONDITION;
        }

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
        long expectedSequence = indexSequence + step;

        if (results == null) {
            results = new ArrayList<P>();
        }

        // DUPLICATE_SEQUENCE
        if (window.containsKey(sequence)) {
            LOGGER.debug(String.format("DUPLICATE_SEQUENCE (sequence: %d)", sequence));

            // No results to release, this packet gets dropped
            return ReceiveResult.DUPLICATE_SEQUENCE;
        }

        // DUPLICATE?
        // It's not in the window, but it might be the "initial condition"
        if (sequence.equals(indexSequence)) {
            LOGGER.debug(String.format("DUPLICATE_SEQUENCE (sequence: %d)", sequence));

            return ReceiveResult.DUPLICATE_SEQUENCE;
        }

        // HOLE_FILLED
        if (hasHoles() && fillsAHole(sequence)) {
            LOGGER.debug(String.format("HOLE_FILLED (sequence: %d, hasHoles: %b, fillsAHole: %b)", sequence, hasHoles(), fillsAHole(sequence)));

            window.put(sequence, value);

            // We only want to add the results if the filled hole was the first hole
            long firstHole = holes.isEmpty() ? sequence : holes.iterator().next();
            holes.remove(sequence);

            if (firstHole == sequence) {
                results.addAll(getResultsAfterAndMoveIndex(sequence));
            }

            return ReceiveResult.HOLE_FILLED;
        }

        // EXPECTED_SEQUENCE
        if (indexSequence == INITIAL_CONDITION || sequence.equals(indexSequence + step)) {
            LOGGER.debug(String.format("EXPECTED_SEQUENCE (indexSequence: %d, step: %d, sequence: %d)", indexSequence, step, sequence));

            // Add a single result
            results.add(value);

            indexSequence = sequence;
            window.put(sequence, value);
            window.shrink();

            return ReceiveResult.EXPECTED_SEQUENCE;
        }

        // POSITIVE_HOLE
        if (sequence > expectedSequence) {
            LOGGER.debug(String.format("POSITIVE_HOLE (indexSequence: %d, step: %d, sequence: %d, idealSize: %d)", indexSequence, step, sequence, window.getIdealSize()));

            Set<Long> holes = getHolesBetween(indexSequence, sequence);
            window.put(sequence, value);
            this.holes.addAll(holes);

            // TODO: Reenable
            waitForHole(sequence, holes);

            // No results to release since we just created a hole
            return ReceiveResult.POSITIVE_HOLE;
        }

        // NEGATIVE_HOLE
        if (sequence < indexSequence + step) {
            LOGGER.debug(String.format("NEGATIVE_HOLE (indexSequence: %d, step: %d, sequence: %d, idealSize: %d)", indexSequence, step, sequence, window.getIdealSize()));

            // This sequence is much lower, must be a reset
            if (indexSequence + step - sequence > window.getIdealSize()) {
                LOGGER.warn("Detected a version reset, clearing window!");
                indexSequence = sequence;
                window.clear();
                holes.clear();
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
     * @param indexSequence non-inclusive
     * @param sequence      non-inclusive
     * @return
     */
    protected Set<Long> getHolesBetween(long indexSequence, long sequence) {
        Set<Long> result = new TreeSet<Long>();

        for (long index = indexSequence + step; index < sequence; index++) {
            if (window.containsKey(index)) {
                // not a hole
                continue;
            }

            result.add(index);
        }

        return result;
    }

    /**
     * Resets the indexSequence to an invalid sequence number and clears the stored data in the window.
     */
    public void reset() {
        indexSequence = INITIAL_CONDITION;
        window.clear();
        holes.clear();
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

    public Long getHighestSequence() {
        try {
            return window.lastKey();
        } catch (Exception e) {
            return indexSequence;
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

    /**
     * From this map, get the ones that are still holes
     *
     * @param holes
     * @return
     */
    protected Set<Long> getExistingHoles(Map<Long, Void> holes) {
        return getExistingHoles(holes.keySet());
    }

    /**
     * From this set, get the ones that are still holes
     *
     * @param holes
     * @return
     */
    protected Set<Long> getExistingHoles(Set<Long> holes) {
        Set<Long> result = new TreeSet<Long>();

        for (Long sequence : holes) {
            if (this.holes.contains(sequence)) {
                result.add(sequence);
            }
        }

        return result;
    }

    protected void waitForHole(final Long sequence, final Set<Long> discoveredHoles) {
        if (CollectionUtil.isNullOrEmpty(holes)) {
            return;
        }

        timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                synchronized (SlidingWindow.this) {
                    final Set<Long> existingHoles = getExistingHoles(discoveredHoles);
                    if (CollectionUtil.isNullOrEmpty(existingHoles)) {
                        // All of the holes we were looking for are no longer holes!
                        return;
                    }

                    notifyObserversOfHoles(existingHoles);

                    timer.newTimeout(new TimerTask() {
                        @Override
                        public void run(Timeout timeout) throws Exception {
                            // we need to clean up any holes and just discard them.
                            Set<Long> currentHoles = getExistingHoles(existingHoles);

                            flushAndReleaseHoles(sequence, currentHoles);
                        }
                    }, getHoleTimeoutMillis() * 2, TimeUnit.MILLISECONDS);
                }
            }
        }, getHoleTimeoutMillis(), TimeUnit.MILLISECONDS);
    }

    protected synchronized void flushAndReleaseHoles(Long sequence, Set<Long> existingHoles) {
        this.holes.removeAll(existingHoles);

        List<P> results = getResultsAfterAndMoveIndex(sequence);

        packetsReleasedEvent.notifyObservers(SlidingWindow.this, results);
    }

    private void notifyObserversOfHoles(Set<Long> existingHoles) {
        Set<HoleRange> ranges = buildHoleRanges(existingHoles);

        for (HoleRange range : ranges) {
            holeTimeoutEvent.notifyObservers(this, range);
        }
    }

    /**
     * Will calculate inclusive ranges. For example, if 0 and 2 are holes, then it would return [0,0;2,2]
     * If 0 2 3 were holes, it would return [0,0;2,3]
     *
     * @param existingHoles
     * @return
     */
    protected Set<HoleRange> buildHoleRanges(Set<Long> existingHoles) {
        HoleRange range = new HoleRange(key, INITIAL_CONDITION, INITIAL_CONDITION);
        Set<HoleRange> result = new TreeSet<HoleRange>(HOLE_RANGE_COMPARATOR);
        result.add(range);
        long expectedSequence = INITIAL_CONDITION;
        for (Long sequence : existingHoles) {
            if (expectedSequence == INITIAL_CONDITION) {
                expectedSequence = sequence;
            }

            if (sequence != expectedSequence) {
                // it was a gap!
                range.end = expectedSequence - step;
                range = new HoleRange(key, sequence, sequence);
                result.add(range);
            } else if (range.start == INITIAL_CONDITION) {
                range.start = sequence;
            }

            range.end = sequence;
            expectedSequence = sequence + step;
        }

        return result;
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
    protected boolean hasHoles() {
        return !holes.isEmpty();
    }

    protected boolean fillsAHole(long sequence) {
        return holes.contains(sequence);
    }

    protected List<HoleRange> getHoles(Set<Long> keys) {
        List<HoleRange> holes = new ArrayList<HoleRange>();

        if ((keys.size() == 1) && keys.contains(indexSequence)) {
            // there are no holes?
            return holes;
        }

        long previous = INITIAL_CONDITION;
        for (Long sequence : keys) {
            if (previous == INITIAL_CONDITION) {
                if (sequence > indexSequence && !(indexSequence + step == sequence)) {
                    holes.add(new HoleRange(key, indexSequence + step, sequence - step));
                }

                previous = sequence - step;
            }

            if (previous >= INITIAL_CONDITION && (previous + step != sequence)) {
                HoleRange range = new HoleRange(key, previous + step, sequence - step);
                holes.add(range);
            }

            previous = sequence;
        }

        return holes;
    }

    public List<HoleRange> getHoles() {
        return getHoles(window.keySet());
    }

    protected P getValue(long sequence) {
        return window.get(sequence);
    }

    protected Long getNextHole(long sequence) {
        return getNextValueAfter(holes.iterator(), sequence);
    }

    protected Long getLastValueUntilHole(long sequence) {
        Long nextHole = getNextHole(sequence);
        if (nextHole == null) {
            return window.lastKey();
        }

        return nextHole - step;
    }

    protected static Long getNextValueAfter(Iterator<Long> iterator, long index) {
        while (iterator.hasNext()) {
            long next = iterator.next();
            if (next > index) {
                return next;
            }
        }

        return null;
    }

    protected List<P> getResultsAfterAndMoveIndex(long sequence) {
        if (indexSequence > sequence) {
            throw new IllegalStateException(String.format("The indexSequence must be lower than the passed in value. %s < %s", indexSequence, sequence));
        }

        sequence = getLastValueUntilHole(sequence);

        List<P> result = new ArrayList<P>();
        for (long index = indexSequence + step; index <= sequence; index += step) {
            if (!window.containsKey(index)) {
                continue;
            }

            result.add(window.get(index));
        }

        indexSequence = sequence;

        return result;
    }

    @Override
    protected void onDestroy() {
        timer.stop();
        holeTimeoutEvent.destroy();
    }

}
