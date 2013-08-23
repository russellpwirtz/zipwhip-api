package com.zipwhip.api.signals;

import com.zipwhip.concurrent.MutableObservableFuture;

/**
 * Date: 8/22/13
 * Time: 3:42 PM
 *
 * @author Michael
 * @version 1
 */
public class SubscriptionRequest {

    private MutableObservableFuture<SubscribeResult> future;
    private String sessionKey;
    private String subscriptionId;

    public SubscriptionRequest(String subscriptionId, String sessionKey, MutableObservableFuture<SubscribeResult> future) {
        this.subscriptionId = subscriptionId;
        this.sessionKey = sessionKey;
        this.future = future;
    }

    public MutableObservableFuture<SubscribeResult> getFuture() {
        return future;
    }

    public void setFuture(MutableObservableFuture<SubscribeResult> future) {
        this.future = future;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriptionRequest)) return false;

        SubscriptionRequest request = (SubscriptionRequest) o;

        if (!subscriptionId.equals(request.subscriptionId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return subscriptionId.hashCode();
    }
}
