package com.zipwhip.api.signals.sockets.netty;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.zipwhip.api.signals.commands.SerializingCommand;

@Deprecated
public final class SignalCommandEncoder extends OneToOneEncoder implements ChannelHandler {

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {

		if (msg instanceof SerializingCommand) {
			SerializingCommand cmd = (SerializingCommand) msg;
			return cmd.serialize();
		}

		return null;
	}

}