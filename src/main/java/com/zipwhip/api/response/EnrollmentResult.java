package com.zipwhip.api.response;

/**
 * Holds the result of a call to enrollAccount
 */
public class EnrollmentResult {

    private boolean isCarbonInstalled;
    private boolean isCarbonEnabled;
    String apiKey;
    String apiSecret;
    long deviceId;
    int deviceNumber;

    public boolean isCarbonInstalled() {
        return isCarbonInstalled;
    }

    public void setCarbonInstalled(boolean carbonInstalled) {
        isCarbonInstalled = carbonInstalled;
    }

    public boolean isCarbonEnabled() {
        return isCarbonEnabled;
    }

    public void setCarbonEnabled(boolean carbonEnabled) {
        isCarbonEnabled = carbonEnabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    public int getDeviceNumber() {
        return deviceNumber;
    }

    public void setDeviceNumber(int deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

}
