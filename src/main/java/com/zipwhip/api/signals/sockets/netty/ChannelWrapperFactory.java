package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.executors.CommonExecutorFactory;
import com.zipwhip.executors.CommonExecutorTypes;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Factory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelWrapperFactory.class);

    private int connectTimeoutSeconds = 15;
    private ChannelPipelineFactory channelPipelineFactory;
    private ChannelFactory channelFactory;
    private SignalConnectionBase signalConnection;
    private CommonExecutorFactory executorFactory;

    public ChannelWrapperFactory(ChannelPipelineFactory channelPipelineFactory, ChannelFactory channelFactory, SignalConnectionBase signalConnection) {
        this(channelPipelineFactory, channelFactory, signalConnection, null);
    }

    public ChannelWrapperFactory(ChannelPipelineFactory channelPipelineFactory, ChannelFactory channelFactory, SignalConnectionBase signalConnection, CommonExecutorFactory executorFactory) {
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

        // create the channel
        Channel channel = channelFactory.newChannel(pipeline);

        channel.getConfig().setConnectTimeoutMillis(getConnectTimeoutSeconds() * 1000);
        channel.getConfig().setOption("receiveBufferSize", 1024);
        channel.getConfig().setOption("sendBufferSize", 1024);
//        channel.getConfig().setOption("keepAlive", true);
//        channel.getConfig().setOption("tcpNoDelay", false);   // assuming default?
//        channel.getConfig().setOption("reuseAddress", false); // assuming default?
//        channel.getConfig().setOption("trafficClass", 0x04);    // RELIABILITY

        LOGGER.debug("Created a wrapper for channel: " + channel);

        // This needs to be here for WakeLockAwareExecutors to be passed in for Android.
        ExecutorService executor = null;
        if (executorFactory != null) {
            executor = executorFactory.create(CommonExecutorTypes.EVENTS, "ChannelWrapper");
        }

        ChannelWrapper channelWrapper = new ChannelWrapper(id.incrementAndGet(), channel, signalConnection, executor);

        if (executor != null){
            final ExecutorService finalExecutor = executor;
            channelWrapper.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    finalExecutor.shutdownNow();
                }
            });
        }

        return channelWrapper;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    @Override
    protected void onDestroy() {

    }
}
