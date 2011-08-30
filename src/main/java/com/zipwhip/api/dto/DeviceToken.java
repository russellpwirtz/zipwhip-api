package com.zipwhip.api.dto;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 19, 2009
 * Time: 2:37:47 PM
 * <p/>
 * Represents a user.
 */
public class DeviceToken {

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

}
