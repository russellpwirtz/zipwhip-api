/**
 * 
 */
package com.zipwhip.api.signals.sockets.netty.pipeline.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

/**
 * @author jdinsel
 *
 */
public class RawSocketChannelHandler extends ObservableChannelHandler {

	/**
	 * The entry point for signal traffic
	 * 
	 * @param ctx
	 *            ChannelHandlerContext
	 * @param e
	 *            MessageEvent
	 * @throws Exception
	 */
	@Override
	public void messageReceived(final ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		Object msg = e.getMessage();

		handleMessage(msg);

	}

}