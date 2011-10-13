package com.zipwhip.api.dto;

import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.subscriptions.SubscriptionEntry;

import java.io.Serializable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/12/11
 * Time: 3:37 PM
 *
 * The SignalToken is received via the JMS broker integration of some vendors.
 * It's used in the case of the firehose subscription.
 */
public class SignalToken implements Serializable {

    private static final long serialVersionUID = 5637364893174383L;

    private List<Signal> signals;
    private SubscriptionEntry subscriptionEntry;
    private String mobileNumber;
    private String deviceAddress;

    public List<Signal> getSignals() {
        return signals;
    }

    public void setSignals(List<Signal> signals) {
        this.signals = signals;
    }

    public SubscriptionEntry getSubscriptionEntry() {
        return subscriptionEntry;
    }

    public void setSubscriptionEntry(SubscriptionEntry subscriptionEntry) {
        this.subscriptionEntry = subscriptionEntry;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

}
