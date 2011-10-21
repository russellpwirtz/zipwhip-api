package com.zipwhip.api.signals.commands;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/24/11
 * Time: 4:19 PM
 */
public class SignalVerificationCommand extends Command {

    public static final String ACTION = "signal_verification";

    @Override
    public String toString() {
        return ACTION;
    }

}
