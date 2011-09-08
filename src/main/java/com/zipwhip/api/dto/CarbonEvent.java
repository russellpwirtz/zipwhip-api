package com.zipwhip.api.dto;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: Austin
 * Date: 6/30/11
 * Time: 3:52 PM
 */
public class CarbonEvent implements Serializable {

    private static final long serialVersionUID = 5874121954952365L;

    public String carbonDescriptor;

    public String getCarbonDescriptor() {
        return carbonDescriptor;
    }

    public void setCarbonDescriptor(String carbonDescriptor) {
        this.carbonDescriptor = carbonDescriptor;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder("==> CarbonEvent details:");
        toStringBuilder.append("\nCarbonDescriptor: ").append(carbonDescriptor);
        return toStringBuilder.toString();
    }

}
