package com.zipwhip.api.signals.commands;

import com.zipwhip.signals.message.Action;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/24/11
 * Time: 4:19 PM
 */
public class SignalVerificationCommand extends Command<Long> {

	private static final long serialVersionUID = 1L;
	public static final Action ACTION = Action.SIGNAL_VERIFICATION; // "signal_verification";

	@Override
	public String toString() {
		return ACTION.name();
	}

	@Override
	public Action getAction() {
		return ACTION;
	}
}
