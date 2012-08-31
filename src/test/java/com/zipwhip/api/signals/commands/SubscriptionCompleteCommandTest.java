/**
 * 
 */
package com.zipwhip.api.signals.commands;

import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.signals.message.Action;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

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
