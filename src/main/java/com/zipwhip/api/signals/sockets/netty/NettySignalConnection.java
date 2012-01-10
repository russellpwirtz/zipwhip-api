package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.sockets.netty.pipeline.handler.SocketIoCommandDecoder;
import com.zipwhip.api.signals.sockets.netty.pipeline.handler.SocketIoCommandEncoder;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;

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
                new SimpleChannelHandler() {

                    @Override
                    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent e) throws Exception {

                        Object msg = e.getMessage();

                        if (!(msg instanceof Command)) {

                            LOGGER.warn("Received a message that was not a command!");

                            return;

                        } else if (msg instanceof PingPongCommand) {

                            // We received a PONG, cancel the PONG timeout.
                            receivePong((PingPongCommand) msg);

                            return;

                        } else {

                            // We have activity on the wire, reschedule the next PING
                            if (doKeepalives) {
                                schedulePing(false);
                            }
                        }

                        Command command = (Command) msg;

                        receiveEvent.notifyObservers(this, command);
                    }

                    @Override
                    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

                        LOGGER.debug("channelConnected");

                        reconnectStrategy.start();

                        connectEvent.notifyObservers(this, Boolean.TRUE);

                        super.channelConnected(ctx, e);
                    }

                    @Override
                    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

                        LOGGER.debug("channelClosed");

                        disconnect(Boolean.TRUE);

                        disconnectEvent.notifyObservers(this, networkDisconnect);

                        super.channelClosed(ctx, e);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

                        LOGGER.error(e.toString());

                        exceptionEvent.notifyObservers(this, e.toString());
                    }

                }
        );
    }

}
