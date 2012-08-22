///**
// *
// */
//package com.zipwhip.api.signals.sockets.netty;
//
//import static org.jboss.netty.channel.Channels.pipeline;
//
//import java.net.InetSocketAddress;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.util.Collections;
//import java.util.Map;
//import java.util.concurrent.Callable;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.FutureTask;
//import java.util.concurrent.TimeUnit;
//
//import org.apache.log4j.Logger;
//import org.jboss.netty.bootstrap.ClientBootstrap;
//import org.jboss.netty.channel.ChannelFuture;
//import org.jboss.netty.channel.ChannelPipeline;
//import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
//import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
//import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
//import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
//import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
//import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
//import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
//import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
//
//import com.zipwhip.api.signals.commands.ConnectCommand;
//import com.zipwhip.api.signals.commands.SerializingCommand;
//import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
//import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
//import com.zipwhip.api.signals.sockets.netty.pipeline.handler.ObservableChannelHandler;
//import com.zipwhip.api.signals.sockets.netty.pipeline.handler.WebsocketChannelHandler;
//import com.zipwhip.signals.server.protocol.SocketIoProtocol;
///**
// * @author jdinsel
// *
// */
//public class WebSocketSignalConnection extends SignalConnectionBase {
//
//	private static final Logger LOG = Logger.getLogger(WebSocketSignalConnection.class);
//	private static final int maxContentLength = 65536;
//
//	private long messageId = 0l;
//	private String URL = "ws://signal-server-01.lynnwood.zipwhip.com:80/socket.io/1/websocket";
//	private WebSocketClientHandshaker handshaker = null;
//	private URI uri;
//
//	/**
//	 * Create a new {@code NettySignalConnection} with a default {@code ReconnectStrategy}.
//	 */
//	public WebSocketSignalConnection() {
//		this(new DefaultReconnectStrategy());
//	}
//
//	/**
//	 * Create a new {@code NettySignalConnection}.
//	 *
//	 * @param reconnectStrategy
//	 *            The reconnect strategy to use in the case of socket disconnects.
//	 */
//	public WebSocketSignalConnection(ReconnectStrategy reconnectStrategy) {
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
//		Map<String, String> emptyMap = Collections.emptyMap();
//
//		try {
//			uri = new URI(URL);
//			handshaker = new WebSocketClientHandshakerFactory().newHandshaker(uri, WebSocketVersion.V13, null, false, emptyMap);
//		} catch (URISyntaxException e) {
//			LOG.fatal("Coult not create a URI for " + URL, e);
//		}
//
//		ObservableChannelHandler websocketChannelHandler = new WebsocketChannelHandler();
//		websocketChannelHandler.setConnectEvent(connectEvent);
//		websocketChannelHandler.setDisconnectEvent(disconnectEvent);
//		websocketChannelHandler.setExceptionEvent(exceptionEvent);
//		websocketChannelHandler.setPingEvent(pingEvent);
//		websocketChannelHandler.setReceiveEvent(receiveEvent);
//		websocketChannelHandler.setReconnectStrategy(reconnectStrategy);
//		((WebsocketChannelHandler) websocketChannelHandler).setHandshaker(handshaker);
//
//		// Create a default pipeline implementation.
//		ChannelPipeline pipeline = pipeline();
//
//		// Start off speaking with HttpResponse/Request and allow the handler to convert us over to WebSockets
//		pipeline.addLast("decoder", new HttpResponseDecoder());
//		pipeline.addLast("aggregator", new HttpChunkAggregator(maxContentLength));
//		pipeline.addLast("encoder", new HttpRequestEncoder());
//		pipeline.addLast("handler", websocketChannelHandler);
//
//		return pipeline;
//
//	}
//
//	/**
//	 * We speak in web socket frames, not strings.
//	 */
//	@Override
//	public Future<Boolean> send(SerializingCommand command) {
//
//		String message = constructMessage(command);
//
//		channel.write(new TextWebSocketFrame(message));
//	}
//
//
//	protected String constructMessage(SerializingCommand command) {
//		return constructMessage(command, null);
//	}
//
//	protected String constructMessage(SerializingCommand command, String clientId) {
//
//		String message;
//
//		if (command instanceof ConnectCommand) {
//			message = SocketIoProtocol.connectMessageResponse(command.serialize(), clientId);
//		} else {
//			message = SocketIoProtocol.jsonMessageResponse(messageId++, command.serialize());
//		}
//		return message;
//	}
//
//	@Override
//	public synchronized Future<Boolean> connect() throws Exception {
//
//		// Enforce a single connection
//		if ((channel != null) && channel.isConnected()) {
//			throw new Exception("Tried to connect but we already have a channel connected!");
//		}
//		ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor()));
//		bootstrap.setPipeline(getPipeline());
//
//		final ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(uri.getHost(), uri.getPort()));
//
//		FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {
//
//			@Override
//			public Boolean call() throws Exception {
//
//				channelFuture.syncUninterruptibly();
//
//				channel = channelFuture.getChannel();
//
//				boolean socketConnected = false;
//				networkDisconnect = true; // Assume a network failure will
//				// occur during connect
//
//				if (channelFuture != null) {
//
//					channelFuture.await(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
//					socketConnected = !channelFuture.isCancelled() && channelFuture.isSuccess() && channelFuture.getChannel().isConnected();
//
//					networkDisconnect = socketConnected;
//
//					handshaker.handshake(channel).syncUninterruptibly();
//
//				}
//				return Boolean.valueOf(socketConnected);
//
//			}
//		});
//
//		Executors.newSingleThreadExecutor().execute(task);
//
//		return task;
//	}
//
//	/**
//	 * @return the uRL
//	 */
//	public final String getURL() {
//		return URL;
//	}
//
//	/**
//	 * @param uRL
//	 *            the uRL to set
//	 */
//	public final void setURL(String url) {
//		URL = url;
//	}
//
//}
