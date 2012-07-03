package com.zipwhip.api.signals.commands;

import java.util.List;

import com.zipwhip.signals.message.Action;

public class SubscriptionCompleteCommand extends Command<Object> {

    private static final long serialVersionUID = 1L;

    public static final Action ACTION = Action.SUBSCRIPTION_COMPLETE; // "subscription_complete";

    private final String subscriptionId;

    /**
     * Create a new SubscriptionCompleteCommand
     *
     * @param subscriptionId The id for your subscription.
     * @param channels       The list of channels subscribed to.
     */
    public SubscriptionCompleteCommand(String subscriptionId, List<Object> channels) {
        this.subscriptionId = subscriptionId;
        setCommands(channels);
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public Action getAction() {
        return ACTION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriptionCompleteCommand)) return false;

        SubscriptionCompleteCommand that = (SubscriptionCompleteCommand) o;

        if (subscriptionId != null ? !subscriptionId.equals(that.subscriptionId) : that.subscriptionId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return subscriptionId != null ? subscriptionId.hashCode() : 0;
    }
}
