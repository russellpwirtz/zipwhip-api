package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import org.jboss.netty.channel.ChannelPipelineFactory;

/**
 * Connects to the SignalServer via Netty over a raw socket
 */
public class NettySignalConnection extends SignalConnectionBase {

    /**
     * Create a new {@code NettySignalConnection} with a default {@code ReconnectStrategy} and {@code ChannelPipelineFactory}.
     */
    public NettySignalConnection() {
        this(new DefaultReconnectStrategy());
    }

    /**
     * Create a new {@code NettySignalConnection}
     *
     * @param channelPipelineFactory The Factory to create a Netty pipeline.
     */
    public NettySignalConnection(ChannelPipelineFactory channelPipelineFactory) {
        this(new DefaultReconnectStrategy(), channelPipelineFactory);
    }

    /**
     * Create a new {@code NettySignalConnection} with a default {@code ChannelPipelineFactory}.
     *
     * @param reconnectStrategy The reconnect strategy to use in the case of socket disconnects.
     */
    public NettySignalConnection(ReconnectStrategy reconnectStrategy) {
        this.channelPipelineFactory = new RawSocketIoChannelPipelineFactory(channelIdleTimer);
        this.init(reconnectStrategy);
    }

    /**
     * Create a new {@code NettySignalConnection}
     *
     * @param reconnectStrategy The reconnect strategy to use in the case of socket disconnects.
     * @param channelPipelineFactory The Factory to create a Netty pipeline.
     */
    public NettySignalConnection(ReconnectStrategy reconnectStrategy, ChannelPipelineFactory channelPipelineFactory) {
        this.channelPipelineFactory = channelPipelineFactory;
        this.init(reconnectStrategy);
    }

}
