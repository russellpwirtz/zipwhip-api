package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;

/**
 * Connects to the SignalServer via Netty over a raw socket
 */
public class NettySignalConnection extends SignalConnectionBase {

    /**
     * Create a new {@code NettySignalConnection} with a default {@code ReconnectStrategy}.
     */
    public NettySignalConnection() {
        this(new DefaultReconnectStrategy());
    }

    /**
     * Create a new {@code NettySignalConnection}.
     *
     * @param reconnectStrategy The reconnect strategy to use in the case of socket disconnects.
     */
    public NettySignalConnection(ReconnectStrategy reconnectStrategy) {
        this.channelPipelineFactory = new RawSocketIoChannelPipelineFactory();
        this.init(reconnectStrategy);
    }

}
