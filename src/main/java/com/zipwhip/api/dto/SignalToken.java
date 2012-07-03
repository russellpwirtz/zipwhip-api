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
 * <p/>
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SignalToken)) return false;

        SignalToken that = (SignalToken) o;

        if (deviceAddress != null ? !deviceAddress.equals(that.deviceAddress) : that.deviceAddress != null) return false;
        if (mobileNumber != null ? !mobileNumber.equals(that.mobileNumber) : that.mobileNumber != null) return false;
        if (signals != null ? !signals.equals(that.signals) : that.signals != null) return false;
        if (subscriptionEntry != null ? !subscriptionEntry.equals(that.subscriptionEntry) : that.subscriptionEntry != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = signals != null ? signals.hashCode() : 0;
        result = 31 * result + (subscriptionEntry != null ? subscriptionEntry.hashCode() : 0);
        result = 31 * result + (mobileNumber != null ? mobileNumber.hashCode() : 0);
        result = 31 * result + (deviceAddress != null ? deviceAddress.hashCode() : 0);
        return result;
    }
}
