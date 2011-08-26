package com.zipwhip.api.signals.commands;

import com.zipwhip.api.signals.Signal;

/**
 * @author jed
 * 
 */
public class SignalCommand extends Command {

    public static final String ACTION = "signal";

    private Signal signal;

    /**
     * Create a new SignalCommand
     *
     * @param signal JSONObject representing the signal
     */
    public SignalCommand(Signal signal) {
        this.signal = signal;
    }

    public Signal getSignal() {
        return signal;
    }

}
