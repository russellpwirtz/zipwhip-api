package com.zipwhip.api.signals.important.subscription;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 4:32 PM
 */
public class SignalsConnectParameters implements Serializable {

    private final String sessionKey;
    private final String clientId;

    public SignalsConnectParameters(String sessionKey, String clientId) {
        this.sessionKey = sessionKey;
        this.clientId = clientId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public String getClientId() {
        return clientId;
    }
}
