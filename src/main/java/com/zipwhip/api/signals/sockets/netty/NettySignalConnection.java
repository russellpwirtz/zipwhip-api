package com.zipwhip.api.signals.sockets.netty;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.netty.pipeline.handler.SocketIoCommandDecoder;
import com.zipwhip.api.signals.sockets.netty.pipeline.handler.SocketIoCommandEncoder;

/**
 * Connects to the SignalServer via Netty over a raw socket
 */
public class NettySignalConnection extends SignalConnectionBase {

    public static final int DEFAULT_FRAME_SIZE = 8192;

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
        this.init(reconnectStrategy);
    }

    /*
      * (non-Javadoc)
      *
      * @see com.zipwhip.api.signals.sockets.netty.SignalConnectionBase#getPipeline()
      */
    @Override
    protected ChannelPipeline getPipeline() {

        return Channels.pipeline(
                new DelimiterBasedFrameDecoder(DEFAULT_FRAME_SIZE, Delimiters.lineDelimiter()),
                new StringDecoder(),
                new SocketIoCommandDecoder(),
                new StringEncoder(),
                new SocketIoCommandEncoder(),
                new NettyChannelHandler(this)
        );
    }

}
