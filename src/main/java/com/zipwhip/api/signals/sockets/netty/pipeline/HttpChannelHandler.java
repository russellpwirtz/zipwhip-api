/**
 * 
 */
package com.zipwhip.api.signals.sockets.netty.pipeline;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;

/**
 * @author jdinsel
 *
 */
public class HttpChannelHandler extends ObservableChannelHandler {

	private static final Logger LOG = Logger.getLogger(HttpChannelHandler.class);

	private boolean readingChunks;
	private String message;

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (!readingChunks) {
			HttpResponse response = (HttpResponse) e.getMessage();

			if (LOG.isDebugEnabled()) {
				LOG.debug("STATUS: " + response.getStatus());
				LOG.debug("VERSION: " + response.getProtocolVersion());

				if (!response.getHeaderNames().isEmpty()) {
					for (String name : response.getHeaderNames()) {
						for (String value : response.getHeaders(name)) {
							LOG.debug("HEADER: " + name + " = " + value);
						}
					}
				}
			}

			if (response.isChunked()) {
				readingChunks = true;
				if (LOG.isDebugEnabled()) {
					LOG.debug("CHUNKED CONTENT {");
				}
			} else {
				ChannelBuffer content = response.getContent();
				if (content.readable()) {
					message = content.toString(CharsetUtil.UTF_8);
					handleMessage(message);
				}
			}
		} else {
			// TODO handle chunks blown at us
			HttpChunk chunk = (HttpChunk) e.getMessage();
			if (chunk.isLast()) {
				readingChunks = false;
				LOG.debug("} END OF CHUNKED CONTENT");
			} else {
				LOG.debug(chunk.getContent().toString(CharsetUtil.UTF_8));
			}
		}
	}

	public final String getMessage() {
		return message;
	}
}
