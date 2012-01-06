package com.zipwhip.signals.presence;

import com.zipwhip.signals.address.ClientAddress;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/14/11
 * Time: 12:01 PM
 *
 * A builder for Presence objects.
 */
public class PresenceBuilder {

	protected String ip;
	protected ClientAddress address;
	protected PresenceCategory category = PresenceCategory.NONE;
	protected UserAgent userAgent;
	protected PresenceStatus status;
	protected Boolean connected;
	protected String subscriptionId;
	protected Date lastActive;
	protected PresenceExtraInfo extraInfo;

	public PresenceBuilder ip(String ip) {
		this.ip = ip;
		return this;
	}

	public PresenceBuilder clientAddressClientId(String clientId) {
		address = new ClientAddress(clientId);
		return this;
	}

	public PresenceBuilder category(PresenceCategory category) {
		this.category = category;
		return this;
	}

	public PresenceBuilder userAgentMakeModel(String makeModel) {
		if (userAgent == null) {
			userAgent = new UserAgent();
			userAgent.setProduct(new Product());
		}
		userAgent.setMakeModel(makeModel);
		return this;
	}

	public PresenceBuilder userAgentBuild(String build) {
		if (userAgent == null) {
			userAgent = new UserAgent();
			userAgent.setProduct(new Product());
		}
		userAgent.setBuild(build);
		return this;
	}

	public PresenceBuilder userAgentProductName(ProductLine name) {
		if (userAgent == null) {
			userAgent = new UserAgent();
			userAgent.setProduct(new Product());
		}
		userAgent.getProduct().setName(name);
		return this;
	}

	public PresenceBuilder userAgentProductVersion(String version) {
		if (userAgent == null) {
			userAgent = new UserAgent();
			userAgent.setProduct(new Product());
		}
		userAgent.getProduct().setVersion(version);
		return this;
	}

	public PresenceBuilder userAgentProductBuild(String build) {
		if (userAgent == null) {
			userAgent = new UserAgent();
			userAgent.setProduct(new Product());
		}
		userAgent.getProduct().setBuild(build);
		return this;
	}

	public PresenceBuilder status(PresenceStatus status) {
		this.status = status;
		return this;
	}

	public PresenceBuilder isConnected(boolean connected) {
		this.connected = Boolean.valueOf(connected);
		return this;
	}

	public PresenceBuilder subscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
		return this;
	}

	public PresenceBuilder lastActive(Date lastActive) {
		this.lastActive = lastActive;
		return this;
	}

	public PresenceBuilder userExtra(String key, Object value) {
		if (extraInfo == null) {
			extraInfo = new PresenceExtraInfo();
		}
		extraInfo.put(key, value);
		return this;
	}

	public Presence build() {
		return new Presence(this);
	}

}
