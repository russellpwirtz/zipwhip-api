package com.zipwhip.signals;

import com.zipwhip.concurrent.HoleRange;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.timers.Timer;
import com.zipwhip.util.BufferedRunnable;
import com.zipwhip.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 10/4/13
 * Time: 10:42 AM
 */
public class DefaultVersionRange implements VersionRange {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultVersionRange.class);

    private final ObservableHelper<HoleRange> holeDetectedEvent =
            new ObservableHelper<HoleRange>("SignalManager/Event", SimpleExecutor.getInstance());

    private final Timer timer;

    private String key;
    private BufferedRunnable holeDetectedEventRunnable;
    protected List<Value> list = new LinkedList<Value>();
    private Set<Long> holes = new TreeSet<Long>();

    public DefaultVersionRange(Timer timer) {
        this.timer = timer;
        this.holeDetectedEventRunnable = new BufferedRunnable(this.timer, this.notifyHoles, 3, TimeUnit.SECONDS);
    }

    public DefaultVersionRange(Timer timer, String key) {
        this(timer);
        this.key = key;
    }

    @Override
    public synchronized boolean add(long version) {
        Value value = new Value(version);

        if (list.contains(value)) {
            return false;
        }

        list.add(value);

        // Sort the list so they are in order.
        Collections.sort(list);

        // Figure out where it was added.
        int index = list.indexOf(value);

        Value toTheRight = value;
        Value toTheLeft = value;

        if (index > 0) {
            toTheLeft = lookBehind(index);
        }

        if (index + 1 < list.size()) {
            toTheRight = lookAhead(index);
        }

        if (toTheLeft.isTrimmedToRight()) {
            // The left claims to be trimmed.
            // This is a NOOP case. Just skip it.
            list.remove(value);

            return true;
        } else if (toTheRight.isTrimmedToLeft()) {
            // The left claims to be trimmed.
            // This is a NOOP case. Just skip it.
            list.remove(value);

            return true;
        }

        // Is there a hole?
        boolean holeToLeft = !(isToTheLeft(toTheLeft, value) || toTheLeft.equals(value));
        boolean holeToRight = !(isToTheRight(toTheRight, value) || toTheRight.equals(value));
        boolean anyHole = holeToLeft || holeToRight;

        if (holeToLeft) {
            // TODO: if over max hole range, reset
            long neighborVersion = value.getLong() - 1;
            while (neighborVersion != toTheLeft.getLong()) {
                LOGGER.debug("Detected negative hole! Version: " + neighborVersion);
                holes.add(neighborVersion);
                neighborVersion--;
            }
        }

        if (holeToRight) {
            // TODO: if over max hole range, reset
            long neighborVersion = value.getLong() + 1;
            while (neighborVersion != toTheRight.getLong()) {
                LOGGER.debug("Detected positive hole! Version: " + neighborVersion);
                holes.add(neighborVersion);
                neighborVersion++;
            }
        }

        if (anyHole) {
            LOGGER.debug("Running holeDetectedEventRunnable for " + holes.size() + " holes.");
            holeDetectedEventRunnable.run();
        }

        autoPruneSignalList();

        return true;
    }

    private final Runnable notifyHoles = new Runnable() {
        @Override
        public void run() {
            synchronized (DefaultVersionRange.this) {
                List<HoleRange> holes = takeHoles();

                for (HoleRange hole : holes) {
                    LOGGER.debug(String.format("Notify holes. Start: %d, End: %d", hole.getStart(), hole.getEnd()));

                    holeDetectedEvent.notifyObservers(DefaultVersionRange.this, hole);
                }
            }
        }
    };

    private boolean isToTheRight(Value toTheRight, Value value) {
        // TODO: is this the correct use of .equals?
        return toTheRight.getLong().equals(value.getLong() + 1);
    }

    private boolean isToTheLeft(Value toTheLeft, Value value) {
        return toTheLeft.getLong().equals(value.getLong() - 1);
    }

    private void autoPruneSignalList() {
        if (CollectionUtil.isNullOrEmpty(list)) {
            return;
        } else if (list.size() <= 1) {
            return;
        }

        Set<Long> thingsToRemove = new TreeSet<Long>();

        for (int index = 0; index < list.size(); index++) {
            Value currentValue = list.get(index);
            Value toTheLeft = lookBehind(index);
            Value toTheRight = lookAhead(index);

            // Verify the begin edge condition

            if (index == 0) {
                if (toTheRight == null) {
                    // It's a hole, or the first one of a list of 1.
                    // We've already verified that the list contains more than 1 item.
                    throw new RuntimeException("This should never happen");
                } else if (isToTheRight(toTheRight, currentValue)) {
                    toTheRight.setTrimmedToLeft(true);

                    if (toTheLeft != null) {
                        toTheLeft.setTrimmedToRight(true);
                    }

                    // Correct sequence
                    // Remove this current position because beginning edges are destructable.
                    thingsToRemove.add(currentValue.getLong());
                    continue;
                } else {
                    // Incorrect sequence
                    // This needs to stay in the list
                    continue;
                }
            }

            if (currentValue.isTrimmedToLeft() && currentValue.isTrimmedToRight()) {
                thingsToRemove.add(currentValue.getLong());
                continue;
            }

            if (currentValue.isTrimmedToRight()) {
                // check left
                if (toTheLeft == null) {
                    // nothing to check.
                    continue;
                }

                if (isToTheLeft(toTheLeft, currentValue)) {
                    toTheLeft.setTrimmedToRight(true);

                    // I'm no longer needed. Trim myself
                    thingsToRemove.add(currentValue.getLong());
                    continue;
                } else {
                    // It's a hole still.
                    continue;
                }
            }

            if (currentValue.isTrimmedToLeft()) {
                // check left
                if (toTheRight == null) {
                    // nothing to check.
                    continue;
                }

                if (isToTheRight(toTheRight, currentValue)) {
                    toTheRight.setTrimmedToLeft(true);

                    // I'm no longer needed. Trim myself
                    thingsToRemove.add(currentValue.getLong());
                    continue;
                } else {
                    // It's a hole still.
                    continue;
                }
            }

            // It's not trimmed at all. Check to see if we can trim?
            if (toTheLeft != null) {
                if (isToTheLeft(toTheLeft, currentValue)) {
                    if (toTheLeft.isTrimmedToRight()) {
                        throw new RuntimeException("Impossible");
                    }

                    if (toTheLeft.isTrimmedToLeft()) {
                        // I can take its place
                        currentValue.setTrimmedToLeft(true);
                        thingsToRemove.add(toTheLeft.getLong());
                        continue;
                    }

                    if (toTheRight == null) {
                        // I have to stay here for the next signal
                        continue;
                    } else if (isToTheRight(toTheRight, currentValue)) {
                        toTheLeft.setTrimmedToRight(true);
                        toTheRight.setTrimmedToLeft(true);
                        thingsToRemove.add(currentValue.getLong());
                        continue;
                    }
                }
            }
        }

        for (Long value : thingsToRemove) {
            list.remove(new Value(value));
        }
    }

    private Value lookAhead(int index) {
        return __safe_get(index + 1);
    }

    private Value lookBehind(int index) {
        return __safe_get(index - 1);
    }

    private Value __safe_get(int index) {
        if (index < 0) {
            return null;
        } else if (index >= list.size()) {
            return null;
        } else if (CollectionUtil.isNullOrEmpty(list)) {
            return null;
        }

        return list.get(index);
    }

    @Override
    public Observable<HoleRange> getHoleDetectedEvent() {
        return holeDetectedEvent;
    }

    @Override
    public Long getHighestVersion() {
        if (CollectionUtil.isNullOrEmpty(list)) {
            return null;
        }

        LOGGER.debug(String.format("Getting highest version out of %d versions. First entry: %s, last entry: %s",
                list.size(), list.get(0).getLong(), list.get(list.size() - 1).getLong()));

        Value value = list.get(list.size() - 1);

        if (value == null) {
            return null;
        }

        return value.getLong();
    }

    @Override
    public List<HoleRange> takeHoles() {
        // Get the current holes.
        synchronized (DefaultVersionRange.this) {
            Collection<Long> dest = new ArrayList<Long>(holes);
            List<HoleRange> result = new ArrayList<HoleRange>();
            holes.clear();

            for (Long version : dest) {
                result.add(new HoleRange(key, version, version));
            }

            return result;
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    protected static class Value implements Comparable<Value> {

        private long value;
        private boolean trimmedToRight;
        private boolean trimmedToLeft;

        public Value() {

        }

        public Value(long value) {
            this.value = value;
        }

        public Long getLong() {
            return value;
        }

        public void setValue(Long value) {
            this.value = value;
        }

        public boolean isTrimmedToLeft() {
            return trimmedToLeft;
        }

        public void setTrimmedToLeft(boolean trimmedToLeft) {
            this.trimmedToLeft = trimmedToLeft;
        }

        public boolean isTrimmedToRight() {
            return trimmedToRight;
        }

        public void setTrimmedToRight(boolean trimmedToRight) {
            this.trimmedToRight = trimmedToRight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Value value1 = (Value) o;

            if (value != value1.value) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return (int) (value ^ (value >>> 32));
        }

        // TODO: validate this comparison
        @Override
        public int compareTo(Value o) {
            if (o == null) {
                return 1;
            }

            return -1 * o.getLong().compareTo(this.getLong());
        }

        @Override
        public String toString() {
            String start;
            String end;

            if (trimmedToLeft) {
                start = "<";
            } else if (trimmedToRight) {
                start = "{";
            } else {
                start = "(";
            }

            if (trimmedToRight) {
                end = ">";
            } else if (trimmedToLeft) {
                end = "}";
            } else {
                end = ")";
            }

            return start + value + end;
        }
    }
}
