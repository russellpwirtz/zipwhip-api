package com.zipwhip.signals.presence;

import com.zipwhip.signals.address.ClientAddress;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * Created by IntelliJ IDEA.
 * 
 * User: Michael
 * 
 * Date: 6/28/11
 * 
 * Time: 2:43 PM
 */

public class Presence implements Serializable {

	// we control the serialisation version
	private static final long serialVersionUID = 10375439476839415L;

	/**
	 * 
	 * A protected constructor for use by the builder.
	 * 
	 * @param builder
	 *            The builder from which to construct this Presence
	 */

	protected Presence(PresenceBuilder builder) {

		ip = builder.ip;
		address = builder.address;
		category = builder.category;
		userAgent = builder.userAgent;
		status = builder.status;
		connected = builder.connected;
		subscriptionId = builder.subscriptionId;
		lastActive = builder.lastActive;
		extraInfo = builder.extraInfo;
	}

	/**
	 * 
	 * IP address of the client
	 */
	private String ip;

	/**
	 * 
	 * A way to uniquely call you
	 */
	private ClientAddress address;

	/**
	 * 
	 * Tablet, Phone, Browser, none
	 */
	private PresenceCategory category = PresenceCategory.NONE;

	/**
	 * 
	 * Some user agent string like a browser, that tells all apps installed and versions of apps.
	 */
	private UserAgent userAgent;

	/**
	 * 
	 * Status online, busy, away, invisible, offline
	 */
	private PresenceStatus status;

	/**
	 * 
	 * Connected
	 */
	private Boolean connected;

	/**
	 * 
	 * The subscriptionId
	 */
	private String subscriptionId;

	/**
	 * 
	 * Last active Date+Time
	 */
	private Date lastActive;

	/**
	 * 
	 * A way to add undefined key/value data.
	 */
	private PresenceExtraInfo extraInfo;

	public Presence() {

	}

	public PresenceStatus getStatus()
	{
		return status;
	}

	public void setStatus(PresenceStatus status)
	{
		this.status = status;
	}

	public Boolean getConnected()
	{
		return connected;
	}

	public void setConnected(Boolean connected)
	{
		this.connected = connected;
	}

	public String getSubscriptionId()
	{
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId)
	{
		this.subscriptionId = subscriptionId;
	}

	public String getIp()
	{
		return ip;
	}

	public void setIp(String ip)
	{
		this.ip = ip;
	}

	public ClientAddress getAddress()
	{
		return address;
	}

	public void setAddress(ClientAddress address)
	{
		this.address = address;
	}

	public PresenceCategory getCategory()
	{
		return category;
	}

	public void setCategory(PresenceCategory category)
	{
		this.category = category;
	}

	public UserAgent getUserAgent()
	{
		return userAgent;
	}

	public void setUserAgent(UserAgent userAgent)
	{
		this.userAgent = userAgent;
	}

	public Date getLastActive()
	{
		return lastActive;
	}

	public void setLastActive(Date lastActive)
	{
		this.lastActive = lastActive;
	}

	public PresenceExtraInfo getExtraInfo()
	{
		return extraInfo;
	}

	public void setExtraInfo(PresenceExtraInfo extraInfo)
	{
		this.extraInfo = extraInfo;
	}

}