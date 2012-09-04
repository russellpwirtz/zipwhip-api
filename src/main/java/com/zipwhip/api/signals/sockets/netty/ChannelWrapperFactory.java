package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Factory;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/21/12
 * Time: 1:55 PM
 * <p/>
 * Creates "ChannelWrapper" instances "correctly" (NOTE: you still have to call 'connect' manually).
 */
public class ChannelWrapperFactory extends DestroyableBase implements Factory<ChannelWrapper> {

    private static AtomicLong id = new AtomicLong(0);

    private static final Logger LOGGER = Logger.getLogger(ChannelWrapperFactory.class);

    private ChannelPipelineFactory channelPipelineFactory;
    private ChannelFactory channelFactory;
    private SignalConnectionBase connection;

    public ChannelWrapperFactory(ChannelPipelineFactory channelPipelineFactory, ChannelFactory channelFactory, SignalConnectionBase connection) {
        this.channelPipelineFactory = channelPipelineFactory;
        this.channelFactory = channelFactory;
        this.connection = connection;
    }

    @Override
    public ChannelWrapper create() throws Exception {

        // the pipeline is for the protocol (such as Websocket and/or regular sockets)
        ChannelPipeline pipeline = channelPipelineFactory.getPipeline();

        // the delegate lets the channel talk to the connection (such as disconnected)
        SignalConnectionDelegate delegate = new SignalConnectionDelegate(connection);

        // add the channelHandler to the pipeline
        pipeline.addLast("nettyChannelHandler", new NettyChannelHandler(delegate));

        // create the channel
        Channel channel = channelFactory.newChannel(pipeline);

        LOGGER.debug("Created a wrapper for channel: " + channel);

        ChannelWrapper channelWrapper = new ChannelWrapper(id.incrementAndGet(), channel, delegate);

        delegate.setChannelWrapper(channelWrapper);

        return channelWrapper;
    }

    @Override
    protected void onDestroy() {
        if (channelPipelineFactory instanceof Destroyable) {
            ((Destroyable) channelPipelineFactory).destroy();
        }
    }
}
