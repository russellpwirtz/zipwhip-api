package com.zipwhip.api.dto;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/16/11
 * Time: 10:22 AM
 */
public class TransmissionState implements Serializable {

    private static final long serialVersionUID = 5874121985262365L;

    String enumType;
    String name;

    public String getEnumType() {
        return enumType;
    }

    public void setEnumType(String enumType) {
        this.enumType = enumType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
