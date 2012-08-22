package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.*;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 5/31/12
 * Time: 5:22 PM
 */
public class NettyChannelHandler extends SimpleChannelHandler {

    protected static final Logger LOGGER = Logger.getLogger(NettyChannelHandler.class);

    private SignalConnectionDelegate delegate;

    public NettyChannelHandler(SignalConnectionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent e) throws Exception {

        Object msg = e.getMessage();

        if (!(msg instanceof Command)) {

            LOGGER.warn("Received a message that was not a command!");

            return;

        } else if (msg instanceof PingPongCommand) {

            // We received a PONG, cancel the PONG timeout.
            delegate.receivePong((PingPongCommand) msg);

            return;

        } else {

            // TODO: when we have the "Keep alive channel handler" in place, we won't have to do this.
//            // We have activity on the wire, reschedule the next PING
//            if (delegate.doKeepalives) {
//                delegate.schedulePing(false);
//            }
        }

        Command command = (Command) msg;

        delegate.notifyReceiveEvent(this, command);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        LOGGER.debug("channelConnected");
//        delegate.notifyConnect(this, Boolean.TRUE);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        LOGGER.debug("channelClosed, disconnecting...");

        delegate.disconnect(Boolean.TRUE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        LOGGER.error("Caught exception on channel, disconnecting... ", e.getCause());

        delegate.notifyException(this, e.toString());

        delegate.disconnect(Boolean.TRUE);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        LOGGER.debug("channelDisconnected, just logging...");
    }

    @Override
    public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        LOGGER.debug("channelUnbound, just logging...");
    }

}
