/**
 * 
 */
package com.zipwhip.api.signals.commands;

import java.util.List;

import com.zipwhip.signals.message.Action;

/**
 * @author jdinsel
 *
 */
public class BackfillCommand extends Command<Long> {
	private static final long serialVersionUID = 1L;

	public static final Action ACTION = Action.BACKFILL; // "backfill";


	public BackfillCommand(List<Long> command) {
		this.command = command;
	}

	@Override
	public Action getAction() {
		return ACTION;
	}
}
