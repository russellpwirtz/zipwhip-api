package com.zipwhip.api.signals.sockets;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/28/12
 * Time: 3:00 PM
 */
public enum SignalProviderState {
    NONE,

    CONNECTING,

    CONNECTED,

    AUTHENTICATED,

    DISCONNECTED,

    DISCONNECTING
}
