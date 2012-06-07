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

    private SignalConnectionBase signalConnection;

    public NettyChannelHandler(SignalConnectionBase signalConnection) {
        this.signalConnection = signalConnection;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent e) throws Exception {

        Object msg = e.getMessage();

        if (!(msg instanceof Command)) {

            LOGGER.warn("Received a message that was not a command!");

            return;

        } else if (msg instanceof PingPongCommand) {

            // We received a PONG, cancel the PONG timeout.
            signalConnection.receivePong((PingPongCommand) msg);

            return;

        } else {

            // We have activity on the wire, reschedule the next PING
            if (signalConnection.doKeepalives) {
                signalConnection.schedulePing(false);
            }
        }

        Command command = (Command) msg;

        signalConnection.receiveEvent.notifyObservers(this, command);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

        LOGGER.debug("channelConnected");

        signalConnection.reconnectStrategy.start();

        signalConnection.connectEvent.notifyObservers(this, Boolean.TRUE);

        super.channelConnected(ctx, e);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

        LOGGER.debug("channelClosed, disconnecting...");

        signalConnection.disconnect(Boolean.TRUE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

        LOGGER.error("Caught exception on channel, disconnecting... ", e.getCause());

        signalConnection.exceptionEvent.notifyObservers(this, e.toString());

        signalConnection.disconnect(Boolean.TRUE);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelDisconnected(ctx, e);
        LOGGER.debug("channelDisconnected, just logging...");
    }

    @Override
    public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelUnbound(ctx, e);
        LOGGER.debug("channelUnbound, just logging...");
    }

}
