/**
 * 
 */
package com.zipwhip.api.signals.commands;

import java.util.List;

import com.zipwhip.api.signals.Signal;
import com.zipwhip.signals.message.Action;

/**
 * @author jdinsel
 *
 */
public class MessageCommand extends Command<Signal> {

	private static final long serialVersionUID = 1L;
	private static final Action action = Action.MESSAGE;

	private String id;
	private List<Signal> data;

	public MessageCommand(String id, List<Signal> data) {
		this.id = id;
		this.data = data;
	}

	@Override
	public Action getAction() {
		return action;
	}

	public final String getId() {
		return id;
	}

	public final void setId(String id) {
		this.id = id;
	}

	public final List<Signal> getData() {
		return data;
	}

	public final void setData(List<Signal> data) {
		this.data = data;
	}
}
