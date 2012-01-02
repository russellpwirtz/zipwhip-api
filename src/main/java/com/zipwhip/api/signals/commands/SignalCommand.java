package com.zipwhip.api.signals.commands;

import com.zipwhip.api.signals.Signal;
import com.zipwhip.signals.message.Action;

/**
 * @author jed
 * 
 */
public class SignalCommand extends Command {

	private static final long serialVersionUID = 1L;

	public static final Action ACTION = Action.SIGNAL;// "signal";

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

	@Override
	public Action getAction() {
		return ACTION;
	}
}
