package com.zipwhip.signals.presence;

import com.zipwhip.signals.address.Address;
import com.zipwhip.signals.address.ChannelAddress;
import com.zipwhip.signals.mail.ClientInfo;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: David Davis
 * Date: 7/13/11
 * Time: 3:37 PM
 */
public abstract class PresenceProviderBase implements PresenceProvider {

	@Override
	public List<Presence> listByAddress(Address address) {
		throw new RuntimeException("listByAddress not implemented in subclass");
	}

	@Override
	public ClientInfo get(Presence presence) {
		throw new RuntimeException("get not implemented in subclass");
	}

	@Override
	public void put(Presence presence)
	{
		throw new RuntimeException("put not implemented in subclass");

	}

	@Override
	public void put(PresenceGroup presenceGroup)
	{
		throw new RuntimeException("put not implemented in subclass");

	}

	@Override
	public void removeFromChannels(PresenceGroup presenceGroup, List<ChannelAddress> channels)
	{
		throw new RuntimeException("removeFromChannels not implemented in subclass");

	}

	@Override
	public PresenceGroup get(String clientId)
	{
		throw new RuntimeException("get not implemented in subclass");
	}
}
