package com.zipwhip.api.signals.commands;

import java.util.List;

import com.zipwhip.signals.message.Action;

/**
 * @author jed
 * 
 */
@Deprecated
public class BacklogCommand extends Command<SignalCommand> {

	private static final long serialVersionUID = 1L;
	public static final Action ACTION = Action.BACKLOG;

	public BacklogCommand(List<SignalCommand> command) {
		this.command = command;
	}

	@Override
	public Action getAction() {
		return ACTION;
	}

}
