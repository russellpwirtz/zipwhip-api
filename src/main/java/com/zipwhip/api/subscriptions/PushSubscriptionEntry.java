package com.zipwhip.api.subscriptions;

import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA. Date: Jul 19, 2009 Time: 5:20:02 PM
 */
public class PushSubscriptionEntry extends SubscriptionEntry {

    public PushSubscriptionEntry() {
        super("push", null);
    }

    public PushSubscriptionEntry(String url) {
        this();
        this.setSignalFilters("/signal");
        this.setURL(url);
    }

    public void setURL(String url) {
        this.setEncodedSubscriptionSettings("{url:" + JSONObject.quote(url) + "}");
    }

}
