package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.sockets.netty.pipeline.handler.SocketIoCommandDecoder;
import com.zipwhip.api.signals.sockets.netty.pipeline.handler.SocketIoCommandEncoder;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/21/12
 * Time: 4:01 PM
 *
 * The pipeline for raw sockets.
 */
public class RawSocketIoChannelPipelineFactory implements ChannelPipelineFactory {

    public static final int DEFAULT_FRAME_SIZE = 8192;

    /*
      * (non-Javadoc)
      *
      * @see com.zipwhip.api.signals.sockets.netty.SignalConnectionBase#getPipeline()
      */
    @Override
    public ChannelPipeline getPipeline() {
        return Channels.pipeline(
                new DelimiterBasedFrameDecoder(DEFAULT_FRAME_SIZE, Delimiters.lineDelimiter()),
                new StringDecoder(),
                new SocketIoCommandDecoder(),
                new StringEncoder(),
                new SocketIoCommandEncoder()
        );
    }
}
