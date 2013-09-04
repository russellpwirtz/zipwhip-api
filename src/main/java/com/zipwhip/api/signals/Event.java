package com.zipwhip.api.signals;

import com.zipwhip.signals.timeline.TimelineEvent;

import java.io.Serializable;
import java.util.Set;

/**
 * Date: 9/3/13
 * Time: 4:34 PM
 *
 * @author Michael
 * @version 1
 */
public class Event<T extends Serializable> implements TimelineEvent {

    private static final long serialVersionUID = -392586795960592389L;

    private final long timestamp;
    private final Set<String> subscriptionIds;
    private final T data;

    public Event(long timestamp, Set<String> subscriptionIds, T data) {
        this.timestamp = timestamp;
        this.subscriptionIds = subscriptionIds;
        this.data = data;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public Set<String> getSubscriptionIds() {
        return subscriptionIds;
    }

    public T getData() {
        return data;
    }
}
