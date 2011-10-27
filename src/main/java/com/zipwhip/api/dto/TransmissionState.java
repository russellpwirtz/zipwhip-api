package com.zipwhip.api.dto;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/16/11
 * Time: 10:22 AM
 */
public enum TransmissionState implements Serializable {

    PREPPING,

    ACCEPTED,

    QUEUED,

    DELIVERED,

    ERROR,

    UNKNOWN;

    public static TransmissionState parse(String data) {

        if ("PREPPING".equals(data)) {
            return PREPPING;
        }
        if ("ACCEPTED".equals(data)) {
            return ACCEPTED;
        }
        if ("QUEUED".equals(data)) {
            return QUEUED;
        }
        if ("DELIVERED".equals(data)) {
            return DELIVERED;
        }
        if ("ERROR".equals(data)) {
            return ERROR;
        }

        return UNKNOWN;
    }

    /**
     * Tells you if a given state can change.
     *
     * @param state The TransmissionState to check.
     * @return {@code true} if the state id DELIVERED or ERROR, otherwise {@code false}
     */
    public static boolean isInFinalState(TransmissionState state) {
        return state == DELIVERED || state == ERROR;
    }

}
