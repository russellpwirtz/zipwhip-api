package com.zipwhip.api.signals;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/1/11 Time: 3:07 PM
 */
public class SignalEvent {

    /**
     * The set of signals that occurred.
     */
    List<Signal> signals;

    public SignalEvent(List<Signal> signals) {
        this.signals = signals;
    }

    public List<Signal> getSignals() {
        return signals;
    }

    public void setSignals(List<Signal> signals) {
        this.signals = signals;
    }

}
