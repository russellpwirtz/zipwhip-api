package com.zipwhip.api.signals;

import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.SocketSignalProvider;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.util.Factory;
import org.jboss.netty.channel.ChannelPipelineFactory;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/12/11 Time: 6:54 PM
 * <p/>
 * Create signalProviders that connect via Sockets
 */
public class SocketSignalProviderFactory implements Factory<SignalProvider> {

    private ReconnectStrategy reconnectStrategy = null;
    private ChannelPipelineFactory channelPipelineFactory = null;

    private SocketSignalProviderFactory() {

    }

    public static SocketSignalProviderFactory newInstance() {
        return new SocketSignalProviderFactory();
    }

    @Override
    public SignalProvider create() {

        NettySignalConnection connection = new NettySignalConnection(reconnectStrategy, channelPipelineFactory);

//        if (reconnectStrategy != null) {
//            connection.setReconnectStrategy(reconnectStrategy);
//        }

        return new SocketSignalProvider(connection);
    }

    public SocketSignalProviderFactory reconnectStrategy(ReconnectStrategy reconnectStrategy) {
        this.reconnectStrategy = reconnectStrategy;
        return this;
    }

    public SocketSignalProviderFactory channelPipelineFactory(ChannelPipelineFactory channelPipelineFactory) {
        this.channelPipelineFactory = channelPipelineFactory;
        return this;
    }

}
