package com.zipwhip.api.signals.sockets.netty;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.JsonSignalCommandParser;
import com.zipwhip.util.Parser;

@Deprecated
public final class MessageDecoder extends OneToOneDecoder {

	private Parser<String, Command<?>> parser;

	public MessageDecoder() {
		this(new JsonSignalCommandParser());
	}

	public MessageDecoder(Parser<String, Command<?>> parser) {
		this.parser = parser;
	}

	@Override
	protected Object decode(ChannelHandlerContext channelHandlerContext, Channel channel, Object o) throws Exception {

		// comes in as a string, leaves as a SignalCommand
		if (o instanceof String) {
			if (parser == null) {
				return o;
			}

			return parser.parse((String) o);
		}

		return o;
	}

}
