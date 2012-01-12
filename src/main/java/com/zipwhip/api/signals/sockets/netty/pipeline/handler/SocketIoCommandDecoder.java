package com.zipwhip.api.signals.sockets.netty.pipeline.handler;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.JsonSignalCommandParser;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.signals.server.protocol.SocketIoProtocol;
import com.zipwhip.util.Parser;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 1/9/12
 * Time: 4:39 PM
 */
public class SocketIoCommandDecoder extends OneToOneDecoder {

    private static final Logger LOGGER = Logger.getLogger(SocketIoCommandDecoder.class);

    private Parser<String, Command<?>> commandParser;

    public SocketIoCommandDecoder() {
        this(new JsonSignalCommandParser());
    }

    public SocketIoCommandDecoder(Parser<String, Command<?>> parser) {
        this.commandParser = parser;
    }

    @Override
    protected Object decode(ChannelHandlerContext channelHandlerContext, Channel channel, Object o) throws Exception {

        Command command = null;

        // comes in as a SocketIO command, leaves as a SignalCommand
        if (o instanceof String) {

            if (commandParser == null) {
                return o;
            }

            String message = (String) o;

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("SocketIO message: " + message);
            }

            // We have a Socket.IO JSON message, try to parse it to a command
            if (SocketIoProtocol.isJsonMessageCommand(message)) {

                String extractedCommand = SocketIoProtocol.extractCommand(message);

                try {

                    command = commandParser.parse(extractedCommand);

                } catch (Exception ex) {

                    LOGGER.fatal("Could not extract command from " + extractedCommand, ex);
                }
            }

            // We have a Socket.IO HeartBeat message, convert it to a PingPongCommand
            else if (SocketIoProtocol.isHeartBeatCommand(message)) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Received a HeartBeat");
                }

                // We received a PONG, cancel the PONG timeout.
                command = PingPongCommand.getShortformInstance();
            }

            // A non-JSON Socket.IO command
            else if (SocketIoProtocol.isMessageCommand(message)) {

                String extractedCommand = SocketIoProtocol.extractCommand(message);

                if (PingPongCommand.getShortformInstance().serialize().equals(extractedCommand)) {
                    command = PingPongCommand.getShortformInstance();
                }
            }

            // We are assuming that this is a Socket.IO connect command
            else {

                // convert and notify that a connection response was received
                String[] params = message.split(":");

                if ((params != null) && (params.length >= 3)) {

                    String clientId = params[0];

                    Map<String, Long> map = new HashMap<String, Long>();

                    map.put("heartbeat", Long.valueOf(params[1]));
                    map.put("disconnect", Long.valueOf(params[2]));

                    command = new ConnectCommand(clientId, map);
                }
            }
        }

        return command;
    }

}
