package com.zipwhip.api.dto;

import java.io.Serializable;

/**
 * Holds the result of a call to enrollAccount
 */
public class EnrollmentResult implements Serializable {

    private static final long serialVersionUID = 45732985435609L;

    private boolean isCarbonInstalled;
    private boolean isCarbonEnabled;
    private int deviceNumber;

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

    public int getDeviceNumber() {
        return deviceNumber;
    }

    public void setDeviceNumber(int deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder("==> EnrollmentResult details:");
        toStringBuilder.append("\nIsCarbonInstalled: ").append(isCarbonEnabled());
        toStringBuilder.append("\nIsCarbonEnabled: ").append(isCarbonEnabled);
        toStringBuilder.append("\nDeviceNumber: ").append(deviceNumber);

        return toStringBuilder.toString();
    }

}
