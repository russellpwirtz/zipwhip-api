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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnrollmentResult)) return false;

        EnrollmentResult that = (EnrollmentResult) o;

        if (deviceNumber != that.deviceNumber) return false;
        if (isCarbonEnabled != that.isCarbonEnabled) return false;
        if (isCarbonInstalled != that.isCarbonInstalled) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (isCarbonInstalled ? 1 : 0);
        result = 31 * result + (isCarbonEnabled ? 1 : 0);
        result = 31 * result + deviceNumber;
        return result;
    }
}
