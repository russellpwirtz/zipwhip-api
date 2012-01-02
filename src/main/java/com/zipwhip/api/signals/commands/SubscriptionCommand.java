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
public final class SubscriptionCommand extends Command<String> {

	private static final long serialVersionUID = 1L;
	private static final Action action = Action.SUBSCRIBE;

	private String id;
	private List<String> data;

	public SubscriptionCommand() {
	}

	public SubscriptionCommand(String id, List<String> data) {
		this.id = id;
		this.data = data;
	}

	public Action getAction() {
		return action;
	}

	public final String getId() {
		return id;
	}

	public final void setId(String id) {
		this.id = id;
	}

	public final List<String> getData() {
		return data;
	}

	public final void setData(List<String> data) {
		this.data = data;
	}

}
