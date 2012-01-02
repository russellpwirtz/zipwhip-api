package com.zipwhip.api.signals.commands;

import java.io.Serializable;
import java.util.List;

import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.signals.message.Action;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/12/11 Time: 5:43 PM
 * 
 * This represents a command that the SignalServer will respond to.
 */
public abstract class Command<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	protected List<T> command;

	private VersionMapEntry version;

	public VersionMapEntry getVersion() {
		return version;
	}

	public void setVersion(VersionMapEntry version) {
		this.version = version;
	}

	public List<T> getCommands() {
		return command;
	}

	public void setCommands(List<T> command) {
		this.command = command;
	}

	public abstract Action getAction();

}
