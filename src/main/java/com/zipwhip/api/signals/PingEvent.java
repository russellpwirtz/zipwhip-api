package com.zipwhip.api.signals;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/27/11
 * Time: 11:29 AM
 *
 * An enum of the wire inactive ping and pong events.
 * Used for eventing to clients.
 *
 */
public enum PingEvent {

    PING_SCHEDULED,
    PING_SENT,
    PING_CANCELLED,
    PONG_RECEIVED,
    PONG_CANCELLED,
    PONG_TIMEOUT

}
