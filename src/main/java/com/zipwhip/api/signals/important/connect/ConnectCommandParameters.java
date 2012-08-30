package com.zipwhip.api.signals.important.connect;

import com.zipwhip.signals.presence.Presence;

import java.io.Serializable;
import java.lang.Long;import java.lang.String;import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 11:19 AM
 */
public class ConnectCommandParameters implements Serializable {

    private String clientId;
    private String sessionKey;
    private Map<String, Long> versions;
    private Presence presence;

    public ConnectCommandParameters(String clientId, String sessionKey, Map<String, Long> versions, Presence presence) {
        this.clientId = clientId;
        this.sessionKey = sessionKey;
        this.versions = versions;
        this.presence = presence;
    }

    public ConnectCommandParameters(String clientId, String sessionKey, Map<String, Long> versions) {
        this.clientId = clientId;
        this.sessionKey = sessionKey;
        this.versions = versions;
    }

    public ConnectCommandParameters(String clientId, String sessionKey) {
        this.clientId = clientId;
        this.sessionKey = sessionKey;
    }

    public ConnectCommandParameters(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Map<String, Long> getVersions() {
        return versions;
    }

    public void setVersions(Map<String, Long> versions) {
        this.versions = versions;
    }

    public Presence getPresence() {
        return presence;
    }

    public void setPresence(Presence presence) {
        this.presence = presence;
    }
}
