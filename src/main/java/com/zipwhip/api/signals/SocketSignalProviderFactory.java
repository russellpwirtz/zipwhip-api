package com.zipwhip.api.signals;

import com.zipwhip.api.signals.sockets.SocketSignalProvider;
import com.zipwhip.util.Factory;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/12/11 Time: 6:54 PM
 * 
 * Create signalProviders that connect via Sockets
 */
public class SocketSignalProviderFactory implements Factory<SignalProvider> {

    private SocketSignalProviderFactory() {
    }

    public static SocketSignalProviderFactory newInstance() {
        return new SocketSignalProviderFactory();
    }

    @Override
    public SignalProvider create() throws Exception {
        return new SocketSignalProvider();
    }

}
