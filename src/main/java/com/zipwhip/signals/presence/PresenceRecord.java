package com.zipwhip.signals.presence;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: 3/13/11
 * Time: 4:54 AM
 *
 * Keeps track of who is connected to us.
 *
 */
public class PresenceRecord implements Serializable {

	static final long serialVersionUID = 2807362102221705648L;

	/**
	 * clientId
	 */
	private String clientId;

	/**
	 * ShortPolling, LongPolling, WebSockets?
	 */
	private String connectionStrategy;

	/**
	 * The IP address of the connection.
	 */
	private String remoteHost;

	/**
	 * The date/time of the last activity with the customer.
	 */
	private Date lastActive;

	/**
	 * What name should we show to the UI? (this is defined by the client).
	 */
	private String displayName;

	public PresenceRecord() {

	}

	public PresenceRecord(String clientId, String connectionStrategy, String remoteHost) {
		this();
		this.clientId = clientId;
		this.connectionStrategy = ( connectionStrategy == null ) ? null : connectionStrategy.toLowerCase();
		this.remoteHost = remoteHost;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getConnectionStrategy() {
		return connectionStrategy;
	}

	public void setConnectionStrategy(String connectionStrategy) {
		this.connectionStrategy = connectionStrategy;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public Date getLastActive() {
		return (lastActive == null) ? null : (Date) lastActive.clone();
	}

	public void setLastActive(Date lastActive) {
		this.lastActive = (Date) lastActive.clone();
	}

	public void setActiveNow() {
		this.lastActive = new Date();
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
}
