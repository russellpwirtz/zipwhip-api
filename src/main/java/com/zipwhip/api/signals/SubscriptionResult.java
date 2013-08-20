package com.zipwhip.api.signals;

/**
 * Date: 8/7/13
 * Time: 3:46 PM
 *
 * @author Michael
 * @version 1
 */
public class SubscriptionResult {

    private boolean success;
    private boolean bound;
    private Subscription subscription;

    public SubscriptionResult() {

    }

    public SubscriptionResult(boolean success, boolean bound, Subscription subscription) {
        this.success = success;
        this.subscription = subscription;
        this.bound = bound;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public boolean isBound() {
        return bound;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
    }
}
