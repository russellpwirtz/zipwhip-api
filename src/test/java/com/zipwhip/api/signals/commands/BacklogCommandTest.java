/**
 * 
 */
package com.zipwhip.api.signals.commands;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.zipwhip.signals.message.Action;

/**
 * @author jdinsel
 *
 */
public class BacklogCommandTest {

	private BacklogCommand command;
	
	@Test
	public void test() {
		List<SignalCommand> signalCommands = new ArrayList<SignalCommand>();
		command = new BacklogCommand(signalCommands);
		
		assertEquals(Action.BACKLOG, command.getAction());
		
	}

}
