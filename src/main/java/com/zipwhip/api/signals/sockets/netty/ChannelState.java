package com.zipwhip.api.signals.sockets.netty;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/14/12
 * Time: 2:51 PM
 *
 * Represents the states of the Channel
 */
public enum ChannelState {

    DESTROYED,
    NONE,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED

}
