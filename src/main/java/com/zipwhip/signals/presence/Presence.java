package com.zipwhip.signals.presence;

import com.zipwhip.signals.address.ClientAddress;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Set;

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
     * The timestamp that this was established/saved
     */
    private long timestamp;

    /**
     * IP address of the client
     */
    private String ip;

    /**
     * A way to uniquely call you
     */
    private ClientAddress address;

    /**
     * Some user agent string like a browser, that tells all apps installed and versions of apps.
     */
    private UserAgent userAgent;

    /**
     * Connected
     */
    private Boolean connected;

    /**
     * Last active Date+Time
     */
    private Date lastActive;

    /**
     * All of the end-clients that are connected via this ClientAddress.
     *
     * This allows us to support multiple apps under the same clientAddress (connection to the signal server).
     */
    private Set<PresenceEntry> entries;

    /**
     * A way to add undefined key/value data.
     */
    private Map<String, Object> extraInfo;

    public Presence() {

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

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(UserAgent userAgent) {
        this.userAgent = userAgent;
    }

    public Boolean getConnected() {
        return connected;
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    public Date getLastActive() {
        return lastActive;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }

    public Set<PresenceEntry> getEntries() {
        return entries;
    }

    public void setEntries(Set<PresenceEntry> entries) {
        this.entries = entries;
    }

    public Map<String, Object> getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(Map<String, Object> extraInfo) {
        this.extraInfo = extraInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Presence)) return false;

        Presence presence = (Presence) o;

        if (!address.equals(presence.address)) return false;
        if (!connected.equals(presence.connected)) return false;
        if (extraInfo != null ? !extraInfo.equals(presence.extraInfo) : presence.extraInfo != null) return false;
        if (ip != null ? !ip.equals(presence.ip) : presence.ip != null) return false;
        if (!lastActive.equals(presence.lastActive)) return false;
        if (!entries.equals(presence.entries)) return false;
        if (userAgent != null ? !userAgent.equals(presence.userAgent) : presence.userAgent != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + address.hashCode();
        result = 31 * result + (userAgent != null ? userAgent.hashCode() : 0);
        result = 31 * result + connected.hashCode();
        result = 31 * result + lastActive.hashCode();
        result = 31 * result + entries.hashCode();
        result = 31 * result + (extraInfo != null ? extraInfo.hashCode() : 0);
        return result;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}