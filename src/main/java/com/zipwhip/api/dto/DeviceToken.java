package com.zipwhip.api.dto;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 19, 2009
 * Time: 2:37:47 PM
 * <p/>
 * Represents a user.
 */
public class DeviceToken implements Serializable {

    private static final long serialVersionUID = 5874121954372365L;

    Device device;
    String sessionKey;
    String apiKey;
    String secret;

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder("==> DeviceToken details:");
        toStringBuilder.append("\nDevice: ").append(device.toString());
        toStringBuilder.append("\nSessionKey: ").append(sessionKey);
        toStringBuilder.append("\nApiKey: ").append(apiKey);

        return toStringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceToken)) return false;

        DeviceToken that = (DeviceToken) o;

        if (apiKey != null ? !apiKey.equals(that.apiKey) : that.apiKey != null) return false;
        if (device != null ? !device.equals(that.device) : that.device != null) return false;
        if (secret != null ? !secret.equals(that.secret) : that.secret != null) return false;
        if (sessionKey != null ? !sessionKey.equals(that.sessionKey) : that.sessionKey != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = device != null ? device.hashCode() : 0;
        result = 31 * result + (sessionKey != null ? sessionKey.hashCode() : 0);
        result = 31 * result + (apiKey != null ? apiKey.hashCode() : 0);
        result = 31 * result + (secret != null ? secret.hashCode() : 0);
        return result;
    }
}
