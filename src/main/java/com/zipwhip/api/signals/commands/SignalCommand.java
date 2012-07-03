package com.zipwhip.api.signals.commands;

import com.zipwhip.api.signals.Signal;
import com.zipwhip.signals.message.Action;

/**
 * @author jed
 */
public class SignalCommand extends Command {

    private static final long serialVersionUID = 1L;
    public static final long NOT_BACKFILL_SIGNAL_VERSION = -1l;

    public static final Action ACTION = Action.SIGNAL;// "signal";

    private final Signal signal;
    private boolean isBackfill = false;
    private long maxBackfillVersion = NOT_BACKFILL_SIGNAL_VERSION;

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

    @Override
    public Action getAction() {
        return ACTION;
    }

    /**
     * @return the isBackfill
     */
    public final boolean isBackfill() {
        return isBackfill;
    }

    /**
     * @param isBackfill the isBackfill to set
     */
    public final void setBackfill(boolean isBackfill) {
        this.isBackfill = isBackfill;
    }

    /**
     * @return the maxBackfillVersion
     */
    public final long getMaxBackfillVersion() {
        return maxBackfillVersion;
    }

    /**
     * @param maxBackfillVersion the maxBackfillVersion to set
     */
    public final void setMaxBackfillVersion(long maxBackfillVersion) {
        this.maxBackfillVersion = maxBackfillVersion;
        if (maxBackfillVersion > NOT_BACKFILL_SIGNAL_VERSION)
            setBackfill(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SignalCommand)) return false;

        SignalCommand that = (SignalCommand) o;

        if (isBackfill != that.isBackfill) return false;
        if (maxBackfillVersion != that.maxBackfillVersion) return false;
        if (signal != null ? !signal.equals(that.signal) : that.signal != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = signal != null ? signal.hashCode() : 0;
        result = 31 * result + (isBackfill ? 1 : 0);
        result = 31 * result + (int) (maxBackfillVersion ^ (maxBackfillVersion >>> 32));
        return result;
    }
}
