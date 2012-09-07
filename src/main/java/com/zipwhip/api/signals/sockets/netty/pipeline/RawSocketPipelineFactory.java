/**
 * 
 */
package com.zipwhip.api.signals.sockets.netty.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

/**
 * @author jdinsel
 *
 */
public class RawSocketPipelineFactory implements ChannelPipelineFactory {

	@Override
	public ChannelPipeline getPipeline() throws Exception {

		return Channels.pipeline(
				new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()),
				// new StringToChannelBuffer(),
				new StringDecoder(),
				// new MessageDecoder(),
				// new SignalCommandEncoder(),
				new StringEncoder(), 
				new RawSocketChannelHandler());
	}

}
