/**
 * 
 */
package com.zipwhip.api.signals.commands;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.signals.message.Action;

/**
 * @author jdinsel
 * 
 */
public class SubscriptionCompleteCommandTest {

	private SubscriptionCompleteCommand command;
	private List<Object> channels;

	@Test
	public void testCommand() throws Exception {

		VersionMapEntry map = new VersionMapEntry("key", Long.valueOf(1));
		
		command = new SubscriptionCompleteCommand("123456", channels);
		command.setVersion(map);
	
		assertEquals(Action.SUBSCRIPTION_COMPLETE, command.getAction());
	}

}
