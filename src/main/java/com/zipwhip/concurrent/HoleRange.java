package com.zipwhip.concurrent;

import java.util.ArrayList;
import java.util.List;

public class HoleRange {

    protected String key;
    protected long start;
    protected long end;

    public HoleRange(String key, long start, long end) {
        this.key = key;
        if (start > end) {
            throw new IllegalArgumentException(String.format("Your numbers are reversed. Please track/fix this bug! {key: %s, start: %s, end:%s}", key, start, end));
        }

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