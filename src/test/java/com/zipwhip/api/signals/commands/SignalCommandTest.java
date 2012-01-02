/**
 * 
 */
package com.zipwhip.api.signals.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.zipwhip.api.signals.Signal;
import com.zipwhip.signals.message.Action;

/**
 * @author jdinsel
 *
 */
public class SignalCommandTest {

	private SignalCommand command;
	
	@Test
	public void test() {
		Signal signal = new Signal();
		command = new SignalCommand(signal);
		
		assertEquals(Action.SIGNAL, command.getAction());
	}

}
