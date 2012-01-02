package com.zipwhip.api.signals.sockets.netty;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.netty.pipeline.handler.ObservableChannelHandler;
import com.zipwhip.api.signals.sockets.netty.pipeline.handler.RawSocketChannelHandler;

/**
 * 
 * Connects to the SignalServer via Netty over a raw socket
 */
public class NettySignalConnection extends SignalConnectionBase {

	/**
	 * Create a new {@code NettySignalConnection} with a default {@code ReconnectStrategy}.
	 */
	public NettySignalConnection() {
		this(new DefaultReconnectStrategy());
	}

	/**
	 * Create a new {@code NettySignalConnection}.
	 *
	 * @param reconnectStrategy The reconnect strategy to use in the case of socket disconnects.
	 */
	public NettySignalConnection(ReconnectStrategy reconnectStrategy) {

		this.init(reconnectStrategy);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.zipwhip.api.signals.sockets.netty.SignalConnectionBase#getPipeline()
	 */
	@Override
	protected ChannelPipeline getPipeline() {

		ObservableChannelHandler rawSocketChannelHandler = new RawSocketChannelHandler();
		rawSocketChannelHandler.setConnectEvent(connectEvent);
		rawSocketChannelHandler.setDisconnectEvent(disconnectEvent);
		rawSocketChannelHandler.setExceptionEvent(exceptionEvent);
		rawSocketChannelHandler.setPingEvent(pingEvent);
		rawSocketChannelHandler.setReceiveEvent(receiveEvent);
		rawSocketChannelHandler.setReconnectStrategy(reconnectStrategy);

		return Channels.pipeline(
				// Second arg must be set to false. This tells Netty not to strip the frame delimiter so we can recognise PONGs upstream.
				//	                new DelimiterBasedFrameDecoder(MAX_FRAME_SIZE, false, copiedBuffer(StringToChannelBuffer.CRLF, Charset.defaultCharset())),
				new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()),

				//new StringToChannelBuffer(),
				new StringDecoder(),
				new StringEncoder(),
				rawSocketChannelHandler
				);
	}

}
