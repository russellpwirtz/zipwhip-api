/**
 * 
 */
package com.zipwhip.api.signals.sockets.netty.pipeline.handler;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;

/**
 * @author jdinsel
 *
 */
public class WebsocketChannelHandler extends ObservableChannelHandler {

	private static final Logger LOG = Logger.getLogger(WebsocketChannelHandler.class);
	private WebSocketClientHandshaker handshaker;
	private boolean handshakeCompleted = false;

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		// callback.onDisconnect(this);
		handshakeCompleted = false;
		super.channelClosed(ctx, e);

	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		LOG.debug("messageReceived " + e);

		Channel channel = ctx.getChannel();
		if (!handshaker.isHandshakeComplete()) {
			handshaker.finishHandshake(channel, (HttpResponse) e.getMessage());
			if (LOG.isDebugEnabled()) {
				LOG.debug("WebSocket client connected!");
			}
			return;
		}

		if (e.getMessage() instanceof HttpResponse) {
			HttpResponse response = (HttpResponse) e.getMessage();
			throw new Exception("Unexpected HttpResponse (status=" + response.getStatus() + ", content=" + response.getContent().toString(CharsetUtil.UTF_8) + ")");
		}

		WebSocketFrame frame = (WebSocketFrame) e.getMessage();
		LOG.debug("Received a frame: " + frame);
		if (frame instanceof TextWebSocketFrame) {
			TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
			if (LOG.isDebugEnabled()) {
				LOG.debug("WebSocket Client received message: " + textFrame.getText());
			}
			handleMessage(textFrame.getText());
		} else if (frame instanceof PongWebSocketFrame) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("WebSocket Client received pong");
			}
		} else if (frame instanceof CloseWebSocketFrame) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("WebSocket Client received closing");
			}
			channel.close();
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		final Throwable t = e.getCause();
		LOG.fatal(t);
		e.getChannel().close();
	}

	public final boolean isHandshakeCompleted() {
		return handshakeCompleted;
	}

	/**
	 * @return the handshaker
	 */
	public final WebSocketClientHandshaker getHandshaker() {
		return handshaker;
	}

	/**
	 * @param handshaker
	 *            the handshaker to set
	 */
	public final void setHandshaker(WebSocketClientHandshaker handshaker) {
		this.handshaker = handshaker;
	}
}
