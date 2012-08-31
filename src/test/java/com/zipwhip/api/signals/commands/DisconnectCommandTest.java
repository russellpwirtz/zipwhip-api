/**
 * 
 */
package com.zipwhip.api.signals.commands;

import com.zipwhip.signals.message.Action;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author jdinsel
 *
 */
public class DisconnectCommandTest {

	private DisconnectCommand command;
	
	@Test
	public void test() {
		String host = "hostname";
		int port = 80;
		int reconnectDelay= 101;
		boolean stop = false;
		boolean ban = false;
		command = new DisconnectCommand(host, port, reconnectDelay, stop, ban );
		
		assertEquals(host, command.getHost());
		assertTrue(port == command.getPort());
		assertTrue(reconnectDelay == command.getReconnectDelay());
		assertTrue(stop == command.isStop());
		assertTrue(ban == command.isBan());
		assertEquals(Action.DISCONNECT, command.getAction());
	}

}
