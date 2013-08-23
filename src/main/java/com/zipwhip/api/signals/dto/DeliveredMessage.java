package com.zipwhip.api.signals.dto;

import com.zipwhip.signals.message.Message;

import java.util.Set;

/**
 * Date: 8/22/13
 * Time: 4:46 PM
 *
 * @author Michael
 * @version 1
 */
public class DeliveredMessage {

    private Set<String> subscriptionIds;
    private Message message;

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
}
