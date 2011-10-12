package com.zipwhip.api.subscriptions;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA. Date: Jul 19, 2009 Time: 5:21:03 PM
 */
public abstract class SubscriptionEntry {

    private String signalFilters;
    private String subscriptionKey;
    private String encodedSubscriptionSettings;

    public SubscriptionEntry(String subscriptionKey, String encodedSubscriptionSettings) {
        this.subscriptionKey = subscriptionKey;
        this.encodedSubscriptionSettings = encodedSubscriptionSettings;
    }

    public String getEncodedSubscriptionSettings() {
        return encodedSubscriptionSettings;
    }

    public void setEncodedSubscriptionSettings(String encodedSubscriptionSettings) {
        this.encodedSubscriptionSettings = encodedSubscriptionSettings;
    }

    public String getSignalFilters() {
        return signalFilters;
    }

    public void setSignalFilters(String signalFilters) {
        this.signalFilters = signalFilters;
    }

    public String getSubscriptionKey() {
        return subscriptionKey;
    }

    public void setSubscriptionKey(String subscriptionKey) {
        this.subscriptionKey = subscriptionKey;
    }

    @Override
    public String toString() {

        JSONObject json = new JSONObject();

        try {
            json.put("subscriptionKey", subscriptionKey);
            json.put("encodedSubscriptionSettings", encodedSubscriptionSettings);
            json.put("signalFilters", signalFilters);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

}
