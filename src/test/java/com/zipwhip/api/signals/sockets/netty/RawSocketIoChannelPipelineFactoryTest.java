package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.sockets.netty.pipeline.handler.SocketIoCommandDecoder;
import com.zipwhip.api.signals.sockets.netty.pipeline.handler.SocketIoCommandEncoder;
import junit.framework.Assert;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 8/23/12
 * Time: 3:15 PM
 */
public class RawSocketIoChannelPipelineFactoryTest {

    RawSocketIoChannelPipelineFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new RawSocketIoChannelPipelineFactory();
    }

    @Test
    public void testGetPipeline() throws Exception {
        ChannelPipeline pipeline = factory.getPipeline();
        Assert.assertNotNull(pipeline.get(DelimiterBasedFrameDecoder.class));
        Assert.assertNotNull(pipeline.get(StringDecoder.class));
        Assert.assertNotNull(pipeline.get(StringEncoder.class));
        Assert.assertNotNull(pipeline.get(SocketIoCommandDecoder.class));
        Assert.assertNotNull(pipeline.get(SocketIoCommandEncoder.class));
        Assert.assertNotNull(pipeline.get(IdleStateHandler.class));
    }

}
