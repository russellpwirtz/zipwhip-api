package com.zipwhip.signals.presence;

import com.zipwhip.signals.address.ClientAddress;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * <p/>
 * User: Michael
 * <p/>
 * Date: 6/28/11
 * <p/>
 * Time: 2:43 PM
 */

public class Presence implements Serializable {

    // we control the serialisation version
    private static final long serialVersionUID = 10375439476839415L;

    /**
     * A protected constructor for use by the builder.
     *
     * @param builder The builder from which to construct this Presence
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
     * IP address of the client
     */
    private String ip;

    /**
     * A way to uniquely call you
     */
    private ClientAddress address;

    /**
     * Tablet, Phone, Browser, none
     */
    private PresenceCategory category = PresenceCategory.NONE;

    /**
     * Some user agent string like a browser, that tells all apps installed and versions of apps.
     */
    private UserAgent userAgent;

    /**
     * Status online, busy, away, invisible, offline
     */
    private PresenceStatus status;

    /**
     * Connected
     */
    private Boolean connected;

    /**
     * The subscriptionId
     */
    private String subscriptionId;

    /**
     * Last active Date+Time
     */
    private Date lastActive;

    /**
     * A way to add undefined key/value data.
     */
    private PresenceExtraInfo extraInfo;

    public Presence() {

    }

    public Presence(PresenceCategory category, UserAgent userAgent) {
        this.setCategory(category);
        this.setUserAgent(userAgent);
    }

    public PresenceStatus getStatus() {
        return status;
    }

    public void setStatus(PresenceStatus status) {
        this.status = status;
    }

    public Boolean getConnected() {
        return connected;
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public ClientAddress getAddress() {
        return address;
    }

    public void setAddress(ClientAddress address) {
        this.address = address;
    }

    public PresenceCategory getCategory() {
        return category;
    }

    public void setCategory(PresenceCategory category) {
        this.category = category;
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(UserAgent userAgent) {
        this.userAgent = userAgent;
    }

    public Date getLastActive() {
        return lastActive;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }

    public PresenceExtraInfo getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(PresenceExtraInfo extraInfo) {
        this.extraInfo = extraInfo;
    }

    /*
      * (non-Javadoc)
      *
      * @see java.lang.Object#toString()
      */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Presence [ip=").append(ip).append(", address=").append(address).append(", category=").append(category).append(", userAgent=").append(userAgent)
                .append(", status=").append(status).append(", connected=").append(connected + ", subscriptionId=").append(subscriptionId).append(", lastActive=")
                .append(lastActive).append("]");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Presence)) return false;

        Presence presence = (Presence) o;

        if (address != null ? !address.equals(presence.address) : presence.address != null) return false;
        if (category != presence.category) return false;
        if (connected != null ? !connected.equals(presence.connected) : presence.connected != null) return false;
        if (extraInfo != null ? !extraInfo.equals(presence.extraInfo) : presence.extraInfo != null) return false;
        if (ip != null ? !ip.equals(presence.ip) : presence.ip != null) return false;
        if (lastActive != null ? !lastActive.equals(presence.lastActive) : presence.lastActive != null) return false;
        if (status != presence.status) return false;
        if (subscriptionId != null ? !subscriptionId.equals(presence.subscriptionId) : presence.subscriptionId != null) return false;
        if (userAgent != null ? !userAgent.equals(presence.userAgent) : presence.userAgent != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (userAgent != null ? userAgent.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (connected != null ? connected.hashCode() : 0);
        result = 31 * result + (subscriptionId != null ? subscriptionId.hashCode() : 0);
        result = 31 * result + (lastActive != null ? lastActive.hashCode() : 0);
        result = 31 * result + (extraInfo != null ? extraInfo.hashCode() : 0);
        return result;
    }
}