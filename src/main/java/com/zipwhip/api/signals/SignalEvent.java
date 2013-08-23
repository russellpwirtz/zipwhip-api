package com.zipwhip.api.signals;

import java.io.Serializable;
import java.util.Set;

/**
 * Date: 8/20/13
 * Time: 3:24 PM
 *
 * @author Michael
 * @version 1
 */
public class SignalEvent<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = -4557298514805623759L;

    private final String clientId;
    private final Set<String> subscriptionIds;
    private final T event;

    public SignalEvent(String clientId, Set<String> subscriptionIds, T event) {
        this.event = event;
        this.subscriptionIds = subscriptionIds;
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public Set<String> getSubscriptionIds() {
        return subscriptionIds;
    }

    public T getEvent() {
        return event;
    }
}
