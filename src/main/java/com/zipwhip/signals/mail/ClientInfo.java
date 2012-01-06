package com.zipwhip.signals.mail;

import com.zipwhip.signals.address.Address;
import com.zipwhip.util.LocalDirectory;
import com.zipwhip.util.StringUtil;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: 1/7/11
 * Time: 3:10 PM
 *
 * A per connection data object that helps us persist state.
 */
public class ClientInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String clientId;
	private String toString = null;
	/**
	 * You can put whatever key/value stuff you want in this map. (as long as it's serializable)
	 */
	private Map<String, Object> cache;

	/**
	 * The channels that this connection is subscribed to.
	 *
	 * The KEY here is the subscriptionId, the ADDRESSes here are the actual subscriptions.
	 */
	private LocalDirectory<String, Address> subscriptions;
	//public String subscriptionId;

	@Override
	public String toString() {
		//return StringUtil.join(getClass().getSimpleName(), " ", clientId, " subscriptionId:", subscriptionId);
		if (toString == null) {
			toString = StringUtil.join(getClass().getSimpleName(), " ", clientId);
		}

		return toString;
	}

	public final String getClientId()
	{
		return clientId;
	}

	public final void setClientId(String clientId)
	{
		this.clientId = clientId;
		toString = null;
	}

	public final Map<String, Object> getCache()
	{
		return cache;
	}

	public final void setCache(Map<String, Object> cache)
	{
		this.cache = cache;
	}

	public final LocalDirectory<String, Address> getSubscriptions()
	{
		return subscriptions;
	}

	public final void setSubscriptions(LocalDirectory<String, Address> subscriptions)
	{
		this.subscriptions = subscriptions;
	}

}
