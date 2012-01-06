package com.zipwhip.signals.app;

import java.io.Serializable;

import com.zipwhip.signals.address.Address;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: Dec 11, 2010
 * Time: 4:32:58 PM
 * 
 * A basic class that is sent between actors. Every message must have a command and an address that dictates who the message is destined
 * for.
 */
public interface Message extends Serializable {

	/**
	 * Where is this message going to!
	 *
	 * @return
	 */
	public Address getAddress();

	public void setAddress(Address address);

	/**
	 * Where has this message been? Where is it from?
	 * @return
	 */
	public Headers getHeaders();

	public void setHeaders(Headers headers);

	/**
	 * The body of the message.
	 * @return
	 */
	public com.zipwhip.api.signals.commands.Command<?> getCommand();

	public void setCommand(com.zipwhip.api.signals.commands.Command<?> command);

}
