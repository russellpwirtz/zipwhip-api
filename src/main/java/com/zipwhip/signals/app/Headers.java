package com.zipwhip.signals.app;

import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: Dec 11, 2010
 * Time: 4:33:34 PM
 * 
 * Message headers, useful for routing.
 */
public class Headers implements MapSerializable, Serializable {

	private static final long serialVersionUID = 1L;
	private static final String KEY_VERSION = "version";
	private static final String KEY_SUBSCRIPTION_ID = "subscriptionId";
	private static final String KEY_CLIENT_ID = "clientId";

	/**
	 * What version you've just received due to this event.
	 */
	private long version;

	/**
	 * WHY are you are receiving it. It tells the client which "concurrent login" this is for.
	 */
	private String subscriptionId;

	/**
	 * This gives you a way to "reply" to someone
	 */
	private String clientId;

	/**
	 * toString and result represent data within this object and need to be rebuilt any time the values change
	 */
	private String toString = null;
	private Map<String, Object> result = new HashMap<String, Object>();

	public Headers() {
	}

	public Headers(String clientId) {
		this.clientId = clientId;
		clear();
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
		clear();
	}

	@Override
	public String toString() {

		if (toString == null) {
			toString = StringUtil.join("{version:", Long.valueOf(version), ",subscriptionId:", subscriptionId, "}");
		}

		return toString;
	}

	@Override
	public Map<String, Object> save() {

		if (CollectionUtil.isNullOrEmpty(result)) {
			result.put(KEY_VERSION, Long.valueOf(version));

			if (!StringUtil.isNullOrEmpty(clientId)){
				result.put(KEY_CLIENT_ID, clientId);
			}

			if (!StringUtil.isNullOrEmpty(subscriptionId)){
				result.put(KEY_SUBSCRIPTION_ID, subscriptionId);
			}
		}
		return result;
	}

	@Override
	public void load(Map<String, Object> properties) {
		version = CollectionUtil.getInteger(properties, KEY_VERSION, 0);
		subscriptionId = CollectionUtil.getString(properties, KEY_SUBSCRIPTION_ID);
		clientId = CollectionUtil.getString(properties, KEY_CLIENT_ID);
		clear();
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
		clear();
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
		clear();
	}

	/**
	 * Clear out the objects that may be used to reference this object
	 */
	private void clear()
	{
		toString = null;
		result = null;
	}
}
