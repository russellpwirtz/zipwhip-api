package com.zipwhip.api.signals.sockets.netty.pipeline;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.api.signals.sockets.netty.SignalConnectionDelegate;
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
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object object = e.getMessage();

        if (object instanceof PingPongCommand) {
            delegate.notifyPingEvent(PingEvent.PING_SENT);
        }

        if (ctx == null) {
            // in our unit tests it's null and we don't want to create a Mock context
            return;
        }
        super.writeRequested(ctx, e);
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        if (delegate.isPaused()) {
            LOGGER.error("Paused so ignoring messageReceived?!?!");
            return;
        }

        Object msg = event.getMessage();

        if (!(msg instanceof Command)) {
            LOGGER.warn("Received a message that was not a command!");

            return;
        } else if (msg instanceof PingPongCommand) {
            delegate.receivePong((PingPongCommand) msg);

            return;
        } else if (msg instanceof SignalCommand) {
            if (((SignalCommand) msg).getSignal() != null){
                LOGGER.error("Command: " + ((SignalCommand) msg).getSignal());
            } else {
                LOGGER.error("Command (message): " + ((SignalCommand)msg).toString());
            }
        }

        LOGGER.debug("We got an event. Going to notify the listeners: " + msg);
        delegate.notifyReceiveEvent((Command) msg);
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent event) throws Exception {
        if (delegate.isPaused()) {
            LOGGER.debug("Paused so ignoring idle");
            return;
        }

        Channel channel = event.getChannel();

        LOGGER.debug("channelIdle: " + channel.toString() + ":" + channel.isConnected() + ":" + delegate.isDestroyed());

        if (event.getState() == IdleState.READER_IDLE) {

            LOGGER.debug("Channel READER_IDLE");

            if (channel.isConnected()) {
                LOGGER.warn("PONG timed out closing channel...");
                delegate.disconnectAsyncIfActive(Boolean.TRUE);
            } else {
                LOGGER.error("Received a READER_IDLE event but the channel is not connected.");
            }

        } else if (event.getState() == IdleState.ALL_IDLE) {

            if (channel.isWritable() && channel.isConnected()) {

                LOGGER.debug("Channel ALL_IDLE, sending PING");

                try {
                    delegate.sendAsyncIfActive(PingPongCommand.getShortformInstance());
                } catch (IllegalStateException e) {
                    LOGGER.warn("IllegalStateException on send" , e);
                    // We were probably disconnected
                }  catch (Exception e) {
                    LOGGER.warn("Tried to send a PING but got an exception" , e);
                    delegate.disconnectAsyncIfActive(Boolean.TRUE);
                }
            } else {
                LOGGER.error("Time to send a PING but the channel is not writable, closing channel...");
                delegate.disconnectAsyncIfActive(Boolean.TRUE);
            }
        }
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        if (delegate.isPaused()) {
            LOGGER.debug("Paused so ignoring close event");
            return;
        }

        LOGGER.debug("channelClosed, disconnecting...");
        delegate.disconnectAsyncIfActive(Boolean.TRUE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent event) throws Exception {
        LOGGER.error("Caught exception on channel... ", event.getCause());
        if (event.getCause() != null){
            event.getCause().printStackTrace();
        }

        if (delegate.isPaused()) {
            LOGGER.debug("Paused so ignoring exception");
            return;
        }

        if (delegate.isDestroyed()) {
            // caught an exception but who cares..
            LOGGER.debug("Delegate was destroyed so i'm just going to sit here nicely.");
            return;
        }

        delegate.notifyExceptionAndDisconnect(event.toString());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent event) throws Exception {
        LOGGER.debug("channelConnected, just logging...");
//        Thread.sleep(4000);
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
