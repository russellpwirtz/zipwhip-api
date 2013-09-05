package com.zipwhip.api.signals.dto;

import com.zipwhip.signals.message.Message;
import com.zipwhip.signals.timeline.TimelineEvent;

import java.util.Set;

/**
 * Date: 8/22/13
 * Time: 4:46 PM
 *
 * @author Michael
 * @version 1
 */
public class DeliveredMessage implements TimelineEvent, Comparable<DeliveredMessage> {

    private static final long serialVersionUID = 4614754472272105186L;

    private Set<String> subscriptionIds;
    private Message message;

    public DeliveredMessage() {
    }

    public DeliveredMessage(Message message) {
        this.message = message;
    }

    public Set<String> getSubscriptionIds() {
        return subscriptionIds;
    }

    public void setSubscriptionIds(Set<String> subscriptionIds) {
        this.subscriptionIds = subscriptionIds;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    @Override
    public long getTimestamp() {
        if (message == null) {
            return 0;
        }

        return message.getTimestamp();
    }

    @Override
    public int compareTo(DeliveredMessage o) {
        long timestamp1 = o == null ? 0 : o.getTimestamp();
        long timestamp2 = this.getTimestamp();

        if (timestamp1 == timestamp2) {
            return 0;
        } else if (timestamp1 > timestamp2) {
            return -1;
        } else {
            return 1;
        }
    }
}
