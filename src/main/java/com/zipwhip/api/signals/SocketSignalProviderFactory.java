package com.zipwhip.api.signals;

import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.SocketSignalProvider;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.util.Factory;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/12/11 Time: 6:54 PM
 * <p/>
 * Create signalProviders that connect via Sockets
 */
public class SocketSignalProviderFactory implements Factory<SignalProvider> {

    private ReconnectStrategy reconnectStrategy;

    private SocketSignalProviderFactory() {
    }

    public static SocketSignalProviderFactory newInstance() {
        return new SocketSignalProviderFactory();
    }

    @Override
    public SignalProvider create() {

        NettySignalConnection nettySignalConnection = new NettySignalConnection();

        if (reconnectStrategy != null) {
            nettySignalConnection.setReconnectStrategy(reconnectStrategy);
        }

        return new SocketSignalProvider(nettySignalConnection);
    }

    public SocketSignalProviderFactory reconnectStrategy(ReconnectStrategy reconnectStrategy) {
        this.reconnectStrategy = reconnectStrategy;
        return this;
    }

}
