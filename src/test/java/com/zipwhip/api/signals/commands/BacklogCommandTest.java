/**
 * 
 */
package com.zipwhip.api.signals.commands;

import com.zipwhip.signals.message.Action;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
