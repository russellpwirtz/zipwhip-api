package com.zipwhip.api.signals;

import com.zipwhip.concurrent.DefaultObservableFuture;

import java.util.Set;

/**
 * Date: 7/31/13
 * Time: 10:42 AM
 *
 * @author Michael
 * @version 1
 */
public class Subscription {

    private String sessionKey;
    private String clientId;
    private String subscriptionId;
    private Set<String> channels;
    private boolean bound;

    private DefaultObservableFuture<SubscribeCompleteContent> subscriptionCompleteFuture;
    private DefaultObservableFuture<Void> innerConnectFuture;

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

    public Set<String> getChannels() {
        return channels;
    }

    public void setChannels(Set<String> channels) {
        this.channels = channels;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean isBound() {
        return bound;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
    }

    public DefaultObservableFuture<SubscribeCompleteContent> getSubscriptionCompleteFuture() {
        return subscriptionCompleteFuture;
    }

    public void setSubscriptionCompleteFuture(DefaultObservableFuture<SubscribeCompleteContent> future) {
        this.subscriptionCompleteFuture = future;
    }

    public DefaultObservableFuture<Void> getInnerConnectFuture() {
        return innerConnectFuture;
    }

    public void setInnerConnectFuture(DefaultObservableFuture<Void> innerConnectFuture) {
        this.innerConnectFuture = innerConnectFuture;
    }
}
