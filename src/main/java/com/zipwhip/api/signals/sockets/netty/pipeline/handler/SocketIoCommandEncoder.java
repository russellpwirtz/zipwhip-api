package com.zipwhip.api.signals.sockets.netty.pipeline.handler;

import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.signals.server.protocol.SocketIoProtocol;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 1/9/12
 * Time: 4:40 PM
 */
public class SocketIoCommandEncoder extends OneToOneEncoder implements ChannelHandler {

    protected static final Logger LOGGER = Logger.getLogger(SocketIoCommandEncoder.class);

    private long messageId = 0l;

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {

        String message = null;

        if (msg instanceof ConnectCommand) {

            message = SocketIoProtocol.connectMessageResponse(((ConnectCommand) msg).serialize(), ((ConnectCommand) msg).getClientId());

        } else if (msg instanceof PingPongCommand) {

            message = SocketIoProtocol.heartBeatMessageResponse(messageId, StringUtil.EMPTY_STRING);

        } else if (msg instanceof SerializingCommand) {

            message = SocketIoProtocol.jsonMessageResponse(messageId++, ((SerializingCommand) msg).serialize());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Encoded message:  " + message);
        }

        return message;
    }

}