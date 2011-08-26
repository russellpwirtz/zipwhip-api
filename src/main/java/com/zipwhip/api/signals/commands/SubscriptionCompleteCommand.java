package com.zipwhip.api.signals.commands;

import java.util.List;

public class SubscriptionCompleteCommand extends Command {

    public static final String ACTION = "subscription_complete";

    private String subscriptionId;
    private List<Object> channels;

    /**
     * Create a new SubscriptionCompleteCommand
     * 
     * @param subscriptionId
     *        The id for your subscription.
     * @param channels
     *        The list of channels subscribed to.
     */
    public SubscriptionCompleteCommand(String subscriptionId, List<Object> channels) {
        this.subscriptionId = subscriptionId;
        this.channels = channels;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public List<Object> getChannels() {
        return channels;
    }

}
