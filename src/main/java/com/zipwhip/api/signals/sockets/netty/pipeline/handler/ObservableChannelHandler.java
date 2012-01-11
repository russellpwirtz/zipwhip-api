/**
 * 
 */
package com.zipwhip.api.signals.sockets.netty.pipeline.handler;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.JsonSignalCommandParser;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.signals.server.protocol.SocketIoProtocol;

/**
 * @author jdinsel
 *
 */
public abstract class ObservableChannelHandler extends SimpleChannelUpstreamHandler {

	protected static final Logger LOG = Logger.getLogger(ObservableChannelHandler.class);

	protected ObservableHelper<PingEvent> pingEvent = new ObservableHelper<PingEvent>();
	protected ObservableHelper<Command> receiveEvent = new ObservableHelper<Command>();
	protected ObservableHelper<Boolean> connectEvent = new ObservableHelper<Boolean>();
	protected ObservableHelper<String> exceptionEvent = new ObservableHelper<String>();
	protected ObservableHelper<Boolean> disconnectEvent = new ObservableHelper<Boolean>();

	private ReconnectStrategy reconnectStrategy;

	private static final JsonSignalCommandParser commandParser = new JsonSignalCommandParser();

	protected void handleMessage(Object message) {
		Command command = null;

		if (!(message instanceof Command)) {

			LOG.warn("Received a message that was not a command! " + message);

			// TODO this is also where we receive other commands
			if (SocketIoProtocol.isJsonMessageCommand(message.toString())) {
				// TODO the server is sending us a message. should it really only send a Command object?
				// TODO what JSON capabilities do we have?
				String extractedCommand = SocketIoProtocol.extractCommand(message.toString());
				try {
					command = commandParser.parse(extractedCommand);
				} catch (Exception e) {
					LOG.fatal("Could not extract command from " + extractedCommand, e);
				}
			} else {

				// convert and notify that a connection response was received
				String[] params = message.toString().split(":");
				if ((params != null) && (params.length >= 3)) {
					Map<String, Long> map = new HashMap<String, Long>();
					map.put("heartbeat", Long.valueOf(params[1]));
					map.put("disconnect", Long.valueOf(params[2]));

					command = new ConnectCommand(params[0], map);
					receiveEvent.notifyObservers(this, command);
				}

				return;
			}
		}

		if (command instanceof PingPongCommand) {

			// TODO
			// We received a PONG, cancel the PONG timeout.
			// receivePong((PingPongCommand) msg);

			return;

		} else {

			// TODO can this be moved up into where the Observers live by using the receiveEvent?

			// We have activity on the wire, reschedule the next PING
			// if (doKeepalives) {
			// schedulePing(false);
			// }
		}


		receiveEvent.notifyObservers(this, command);
	}


	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

		LOG.debug("channelConnected");

		reconnectStrategy.start();
		connectEvent.notifyObservers(this, Boolean.TRUE);
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

		LOG.debug("channelClosed");

		// TODO disconnect(true);
		// disconnectEvent.notifyObservers(this, Boolean.valueOf(networkDisconnect));
		disconnectEvent.notifyObservers(this, Boolean.TRUE);
		super.channelClosed(ctx, e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

		LOG.error(e.toString());
		e.getCause().printStackTrace();

		exceptionEvent.notifyObservers(this, e.toString());
	}

	public final ReconnectStrategy getReconnectStrategy() {
		return reconnectStrategy;
	}

	public final void setReconnectStrategy(ReconnectStrategy reconnectStrategy) {
		this.reconnectStrategy = reconnectStrategy;
	}

	public final ObservableHelper<PingEvent> getPingEvent() {
		return pingEvent;
	}

	public final void setPingEvent(ObservableHelper<PingEvent> pingEvent) {
		this.pingEvent = pingEvent;
	}

	public final ObservableHelper<Command> getReceiveEvent() {
		return receiveEvent;
	}

	public final void setReceiveEvent(ObservableHelper<Command> receiveEvent) {
		this.receiveEvent = receiveEvent;
	}

	public final ObservableHelper<Boolean> getConnectEvent() {
		return connectEvent;
	}

	public final void setConnectEvent(ObservableHelper<Boolean> connectEvent) {
		this.connectEvent = connectEvent;
	}

	public final ObservableHelper<String> getExceptionEvent() {
		return exceptionEvent;
	}

	public final void setExceptionEvent(ObservableHelper<String> exceptionEvent) {
		this.exceptionEvent = exceptionEvent;
	}

	public final ObservableHelper<Boolean> getDisconnectEvent() {
		return disconnectEvent;
	}

	public final void setDisconnectEvent(ObservableHelper<Boolean> disconnectEvent) {
		this.disconnectEvent = disconnectEvent;
	}

}
