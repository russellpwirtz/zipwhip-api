package com.zipwhip.api.signals;

import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.executors.CommonExecutorTypes;
import com.zipwhip.api.signals.sockets.SocketSignalProvider;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.executors.CommonExecutorFactory;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.timers.Timer;
import com.zipwhip.util.Factory;
import org.jboss.netty.channel.ChannelPipelineFactory;

import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/12/11 Time: 6:54 PM
 * <p/>
 * Create signalProviders that connect via Sockets
 */
public class SocketSignalProviderFactory implements Factory<SignalProvider> {

    private ReconnectStrategy reconnectStrategy = null;
    private ChannelPipelineFactory channelPipelineFactory = null;
    private CommonExecutorFactory executorFactory = null;
    private SocketAddress address;
    private Timer timer;

    public SocketSignalProviderFactory() {

    }

    public static SocketSignalProviderFactory newInstance() {
        return new SocketSignalProviderFactory();
    }

    @Override
    public SignalProvider create() {
        NettySignalConnection connection = new NettySignalConnection(executorFactory, reconnectStrategy, channelPipelineFactory);

        connection.setConnectTimeoutSeconds(10);

        if (address != null) {
            connection.setAddress(address);
        }

        Executor executor = null;
        if (executorFactory != null){
            executor = executorFactory.create(CommonExecutorTypes.EVENTS, "SignalProvider");
        }

        SocketSignalProvider signalProvider = new SocketSignalProvider(connection, executor, timer);

        if (executor != null){
            final Executor finalExecutor = executor;
            signalProvider.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ((ExecutorService)finalExecutor).shutdownNow();
                }
            });
        }

        return signalProvider;
    }

    public SocketSignalProviderFactory timer(Timer timer) {
        this.timer = timer;
        return this;
    }

    public SocketSignalProviderFactory reconnectStrategy(ReconnectStrategy reconnectStrategy) {
        this.reconnectStrategy = reconnectStrategy;
        return this;
    }

    public SocketSignalProviderFactory channelPipelineFactory(ChannelPipelineFactory channelPipelineFactory) {
        this.channelPipelineFactory = channelPipelineFactory;
        return this;
    }

    public SocketSignalProviderFactory executorFactory(CommonExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
        return this;
    }

    public SocketSignalProviderFactory address(SocketAddress address) {
        this.address = address;
        return this;
    }
}
