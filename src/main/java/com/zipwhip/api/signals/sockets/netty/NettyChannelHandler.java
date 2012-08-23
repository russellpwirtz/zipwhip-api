package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 5/31/12
 * Time: 5:22 PM
 */
public class NettyChannelHandler extends IdleStateAwareChannelHandler {

    protected static final Logger LOGGER = Logger.getLogger(NettyChannelHandler.class);

    private SignalConnectionDelegate delegate;

    public NettyChannelHandler(SignalConnectionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent event) throws Exception {

        Object msg = event.getMessage();

        if (!(msg instanceof Command)) {

            LOGGER.warn("Received a message that was not a command!");
            return;

        } else if (msg instanceof PingPongCommand) {

            delegate.receivePong((PingPongCommand) msg);
            return;
        }

        delegate.notifyReceiveEvent(this, (Command) msg);
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent event) throws Exception {

        Channel channel = event.getChannel();

        assert channel.isConnected() == delegate.isConnected();

        if (event.getState() == IdleState.READER_IDLE) {

            LOGGER.debug("Channel READER_IDLE");

            if (delegate.isConnected()) {
                LOGGER.warn("PONG timed out closing channel...");
                delegate.disconnect(Boolean.TRUE);
            } else {
                LOGGER.error("Received a READER_IDLE event but the channel is not connected.");
            }

        } else if (event.getState() == IdleState.ALL_IDLE) {

            if (channel.isWritable() && delegate.isConnected()) {

                LOGGER.debug("Channel ALL_IDLE, sending PING");

                try {
                    delegate.send(PingPongCommand.getShortformInstance());
                } catch (IllegalStateException e) {
                    // We were probably disconnected
                    assert !delegate.isConnected();
                    return;
                }  catch (Exception e) {
                    LOGGER.warn("Tried to send a PING but got an exception" , e);
                    delegate.disconnect(Boolean.TRUE);
                    return;
                }

                delegate.notifyPingEvent(this, PingEvent.PING_SENT);

            } else {
                LOGGER.error("Time to send a PING but the channel is not writable, closing channel...");
                delegate.disconnect(Boolean.TRUE);
            }
        }
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        LOGGER.debug("channelClosed, disconnecting...");
        delegate.disconnect(Boolean.TRUE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent event) throws Exception {
        LOGGER.error("Caught exception on channel, disconnecting... ", event.getCause());
        delegate.notifyException(this, event.toString());
        delegate.disconnect(Boolean.TRUE);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        LOGGER.debug("channelConnected, just logging...");
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        LOGGER.debug("channelDisconnected, just logging...");
    }

    @Override
    public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        LOGGER.debug("channelUnbound, just logging...");
    }

}
