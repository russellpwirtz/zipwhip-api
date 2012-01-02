/**
 * 
 */
package com.zipwhip.api.signals.sockets.netty;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;

import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.netty.pipeline.handler.ObservableChannelHandler;
import com.zipwhip.api.signals.sockets.netty.pipeline.handler.WebsocketChannelHandler;
/**
 * @author jdinsel
 *
 */
public class WebSocketSignalConnection extends SignalConnectionBase {

	private static final int maxContentLength = 65536;

	/**
	 * Create a new {@code NettySignalConnection} with a default {@code ReconnectStrategy}.
	 */
	public WebSocketSignalConnection() {
		this(new DefaultReconnectStrategy());
	}

	/**
	 * Create a new {@code NettySignalConnection}.
	 * 
	 * @param reconnectStrategy
	 *            The reconnect strategy to use in the case of socket disconnects.
	 */
	public WebSocketSignalConnection(ReconnectStrategy reconnectStrategy) {

		this.init(reconnectStrategy);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.zipwhip.api.signals.sockets.netty.SignalConnectionBase#getPipeline()
	 */
	@Override
	protected ChannelPipeline getPipeline() {

		ObservableChannelHandler websocketChannelHandler = new WebsocketChannelHandler();
		websocketChannelHandler.setConnectEvent(connectEvent);
		websocketChannelHandler.setDisconnectEvent(disconnectEvent);
		websocketChannelHandler.setExceptionEvent(exceptionEvent);
		websocketChannelHandler.setPingEvent(pingEvent);
		websocketChannelHandler.setReceiveEvent(receiveEvent);
		websocketChannelHandler.setReconnectStrategy(reconnectStrategy);
		((WebsocketChannelHandler) websocketChannelHandler).setHost(getHost());

		// Create a default pipeline implementation.
		ChannelPipeline pipeline = pipeline();

		// Start off speaking with HttpResponse/Request and allow the handler to convert us over to WebSockets
		pipeline.addLast("decoder", new HttpResponseDecoder());
		pipeline.addLast("aggregator", new HttpChunkAggregator(maxContentLength));
		pipeline.addLast("encoder", new HttpRequestEncoder());
		pipeline.addLast("handler", websocketChannelHandler);
		return pipeline;

	}

	/**
	 * We speak in web socket frames, not strings.
	 */
	@Override
	public void send(SerializingCommand command) {

		String message = constructMessage(command);

		WebSocketFrame defaultWebSocketFrame = new DefaultWebSocketFrame();
		defaultWebSocketFrame.setData(0, ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8));

		// send this over the wire.
		channel.write(defaultWebSocketFrame);
	}

}
