package com.zipwhip.api.signals.commands;

import com.zipwhip.signals.message.Action;

/**
 * A bidirectional NOOP command.
 * <p/>
 * {action:NOOP}
 * 
 * @author jed
 *
 */
public class NoopCommand extends SerializingCommand {

	private static final long serialVersionUID = 1L;
	public static final Action ACTION = Action.NOOP;

	@Override
	public String serialize() {
		return "{action:'NOOP'}";
	}

	@Override
	public Action getAction() {
		return ACTION;
	}
}
