/**
 * 
 */
package com.zipwhip.api.signals.sockets.netty.pipeline.handler;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameDecoder;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameEncoder;
import org.jboss.netty.util.CharsetUtil;

/**
 * @author jdinsel
 *
 */
public class WebsocketChannelHandler extends ObservableChannelHandler {

	private static final Logger LOG = Logger.getLogger(WebsocketChannelHandler.class);
	private boolean handshakeCompleted = false;
	private String host = "127.0.0.1"; // TODO how to define this?
	private String path = "/socket.io/websocket";

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		// String path = url.getPath();
		// if ((url.getQuery() != null) && (url.getQuery().length() > 0)) {
		// path = url.getPath() + "?" + url.getQuery();
		// }
		if (LOG.isTraceEnabled()) {
			LOG.trace("Channel connected");
		}

		HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
		request.addHeader(Names.UPGRADE, Values.WEBSOCKET);
		request.addHeader(Names.CONNECTION, Values.UPGRADE);
		request.addHeader(Names.HOST, getHost());
		request.addHeader("DEBUG", "channelConnected event");
		request.addHeader(Names.ORIGIN, "http://" + getHost());

		e.getChannel().write(request);
		ctx.getPipeline().replace("encoder", "ws-encoder", new WebSocketFrameEncoder());

		if (LOG.isTraceEnabled())
		{
			LOG.trace("channel converted to send web sockets");
			// channel = e.getChannel();
		}

		super.channelConnected(ctx, e);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		// callback.onDisconnect(this);
		handshakeCompleted = false;
		super.channelClosed(ctx, e);

	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		// Print out the line received from the server.
		if (LOG.isTraceEnabled()) {
			LOG.trace("client received (handshake:" + handshakeCompleted + "): " + e.getMessage());
		}
		if (!handshakeCompleted) {

			if (LOG.isTraceEnabled()) {
				LOG.trace("Performing handshake");
			}

			HttpResponse response = (HttpResponse) e.getMessage();
			final HttpResponseStatus status = new HttpResponseStatus(101, "Web Socket Protocol Handshake");

			final boolean validStatus = response.getStatus().equals(status);
			final boolean validUpgrade = response.getHeader(Names.UPGRADE).equals(Values.WEBSOCKET);
			final boolean validConnection = response.getHeader(Names.CONNECTION).equals(Values.UPGRADE);

			if (!validStatus || !validUpgrade || !validConnection) {
				throw new RuntimeException("Invalid handshake response");
			}

			handshakeCompleted = true;
			ctx.getPipeline().replace("decoder", "ws-decoder", new WebSocketFrameDecoder());
			// callback.onConnect(this);

			if (LOG.isTraceEnabled()) {
				LOG.trace("handshake complete, converted to receive websockets " + this);
			}
			return;
		}

		if (e.getMessage() instanceof HttpResponse) {
			HttpResponse response = (HttpResponse) e.getMessage();
			throw new RuntimeException("Unexpected HttpResponse (status=" + response.getStatus() + ", content=" + response.getContent().toString(CharsetUtil.UTF_8) + ")");
		}

		DefaultWebSocketFrame frame = (DefaultWebSocketFrame) e.getMessage();
		handleMessage(frame.getTextData());

	}

	public final String getHost() {
		return host;
	}

	public final void setHost(String host) {
		this.host = host;
	}

	public final String getPath() {
		return path;
	}

	public final void setPath(String path) {
		this.path = path;
	}

	public final boolean isHandshakeCompleted() {
		return handshakeCompleted;
	}
}
