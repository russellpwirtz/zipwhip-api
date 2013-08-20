package com.zipwhip.api.signals;

import java.util.Set;

/**
 * Date: 7/31/13
 * Time: 10:46 AM
 *
 * @author Michael
 * @version 1
 */
public class SubscriptionCompleteCommand {

    private String sessionKey;
    private String subscriptionId;
    private Set<String> addresses;

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Set<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<String> addresses) {
        this.addresses = addresses;
    }
}
