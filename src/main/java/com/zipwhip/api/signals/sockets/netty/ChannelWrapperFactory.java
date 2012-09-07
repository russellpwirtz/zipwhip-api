package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.sockets.netty.pipeline.NettyChannelHandler;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Factory;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

import java.util.concurrent.ExecutorService;
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
    private SignalConnectionBase signalConnection;
    private Factory<ExecutorService> executorFactory;

    public ChannelWrapperFactory(ChannelPipelineFactory channelPipelineFactory, ChannelFactory channelFactory, SignalConnectionBase signalConnection) {
        this(channelPipelineFactory, channelFactory, signalConnection, null);
    }

    public ChannelWrapperFactory(ChannelPipelineFactory channelPipelineFactory, ChannelFactory channelFactory, SignalConnectionBase signalConnection, Factory<ExecutorService> executorFactory) {
        this.channelPipelineFactory = channelPipelineFactory;
        this.channelFactory = channelFactory;
        this.signalConnection = signalConnection;
        this.executorFactory = executorFactory;
    }

    @Override
    public ChannelWrapper create() {

        // the pipeline is for the protocol (such as Websocket and/or regular sockets)
        ChannelPipeline pipeline;
        try {
            pipeline = channelPipelineFactory.getPipeline();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // the delegate lets the ChannelHandlers talk to the connection (such as pong-received)
        SignalConnectionDelegate delegate = new SignalConnectionDelegate(signalConnection);

        // add the 'business logic' ChannelHandler to the pipeline
        pipeline.addLast("nettyChannelHandler", new NettyChannelHandler(delegate));

        // create the channel
        Channel channel = channelFactory.newChannel(pipeline);

        LOGGER.debug("Created a wrapper for channel: " + channel);

        // This needs to be here for WakeLockAwareExecutors to be passed in for Android.
        ExecutorService executor = null;
        if (executorFactory != null) {
            executor = executorFactory.create();
        }

        ChannelWrapper channelWrapper = new ChannelWrapper(id.incrementAndGet(), channel, delegate, executor);
        channelWrapper.link(delegate);
        delegate.setConnectionHandle(channelWrapper.getConnection());

        return channelWrapper;
    }

    @Override
    protected void onDestroy() {

    }
}
