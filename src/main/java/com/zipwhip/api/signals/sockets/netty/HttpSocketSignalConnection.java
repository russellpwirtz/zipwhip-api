///**
// *
// */
//package com.zipwhip.api.signals.sockets.netty;
//
//import static org.jboss.netty.channel.Channels.pipeline;
//
//import org.jboss.netty.channel.ChannelPipeline;
//import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
//import org.jboss.netty.handler.codec.http.HttpClientCodec;
//import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
//
//import com.zipwhip.api.signals.commands.ConnectCommand;
//import com.zipwhip.api.signals.commands.SerializingCommand;
//import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
//import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
//import com.zipwhip.api.signals.sockets.netty.pipeline.handler.HttpChannelHandler;
//import com.zipwhip.api.signals.sockets.netty.pipeline.handler.ObservableChannelHandler;
//
//import java.util.concurrent.Future;
//
///**
// * @author jdinsel
// *
// */
//public class HttpSocketSignalConnection extends SignalConnectionBase {
//
//	private static final String path = "/socket.io/xhr-polling";
//
//	/**
//	 * Create a new {@code HttpSocketSignalConnection} with a default {@code ReconnectStrategy}.
//	 */
//	public HttpSocketSignalConnection() {
//		this(new DefaultReconnectStrategy());
//	}
//
//	/**
//	 * Create a new {@code HttpSocketSignalConnection}.
//	 *
//	 * @param reconnectStrategy
//	 *            The reconnect strategy to use in the case of socket disconnects.
//	 */
//	public HttpSocketSignalConnection(ReconnectStrategy reconnectStrategy) {
//
//		this.init(reconnectStrategy);
//	}
//
//	/*
//	 * (non-Javadoc)
//	 *
//	 * @see com.zipwhip.api.signals.sockets.netty.SignalConnectionBase#getPipeline()
//	 */
//	@Override
//	protected ChannelPipeline getPipeline() {
//
//		ObservableChannelHandler httpSocketChannelHandler = new HttpChannelHandler();
//		httpSocketChannelHandler.setConnectEvent(connectEvent);
//		httpSocketChannelHandler.setDisconnectEvent(disconnectEvent);
//		httpSocketChannelHandler.setExceptionEvent(exceptionEvent);
//		httpSocketChannelHandler.setPingEvent(pingEvent);
//		httpSocketChannelHandler.setReceiveEvent(receiveEvent);
//		httpSocketChannelHandler.setReconnectStrategy(reconnectStrategy);
//
//		// Create a default pipeline implementation.
//		ChannelPipeline pipeline = pipeline();
//
//		// Start off speaking with HttpResponse/Request and allow the handler to convert us over to WebSockets
//		// pipeline.addLast("decoder", new HttpResponseDecoder());
//		pipeline.addLast("codec", new HttpClientCodec());
//		pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
//		pipeline.addLast("inflater", new HttpContentDecompressor());
//		// pipeline.addLast("encoder", new HttpRequestEncoder());
//		pipeline.addLast("handler", httpSocketChannelHandler);
//
//		return pipeline;
//
//	}
//
//	/**
//	 * We speak in web packets frames, not strings.
//	 */
//	@Override
//	public Future<Boolean> send(SerializingCommand command) {
//
//		// An http polling socket is always in the connect state
////		connectionSent = false;
//
//		// These are always connect commands for this transport
//		ConnectCommand connectCommand = (ConnectCommand) command;
//		String clientId = connectCommand.getClientId();
//
////		String message = path + "/" + constructMessage(command, clientId);
//
////		int idx = message.lastIndexOf('\n');
////		if (idx != -1) {
////			message = message.substring(0, idx);
////		}
////
////		// TODO remove
////		System.out.println("Writing: " + message);
////
////		HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, message);
////		request.setHeader(HttpHeaders.Names.HOST, getHost());
////		request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
////		request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
////		request.setHeader(Names.ORIGIN, "http://" + getHost() + ":" + getPort());
////
////		// send this over the wire.
////		channel.write(request);
//	}
//
//}
