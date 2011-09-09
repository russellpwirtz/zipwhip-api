package com.zipwhip.api;

import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.SocketSignalProviderFactory;
import com.zipwhip.util.Factory;
import com.zipwhip.util.StringUtil;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 7/5/11 Time: 6:24 PM
 * <p/>
 * This factory produces ZipwhipClient's that are authenticated
 */
public class ZipwhipClientFactory implements Factory<ZipwhipClient> {

    private Factory<Connection> connectionFactory;
    private Factory<SignalProvider> signalProviderFactory;

    private ZipwhipClientFactory() {
    }

    private ZipwhipClientFactory(ConnectionFactory connectionFactory, SocketSignalProviderFactory signalProviderFactory) {
        this.connectionFactory = connectionFactory;
        this.signalProviderFactory = signalProviderFactory;
    }

    public static ZipwhipClient createViaUsername(String username, String password) throws Exception {

        ConnectionFactory connectionFactory = ConnectionFactory.newInstance().username(username).password(password);
        SocketSignalProviderFactory signalProviderFactory = SocketSignalProviderFactory.newInstance();

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, signalProviderFactory);

        return zipwhipClientFactory.create();
    }

    public static ZipwhipClient createViaSessionKey(String sessionKey) throws Exception {

        ConnectionFactory connectionFactory = ConnectionFactory.newInstance().sessionKey(sessionKey);
        SocketSignalProviderFactory signalProviderFactory = SocketSignalProviderFactory.newInstance();

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, signalProviderFactory);

        return zipwhipClientFactory.create();
    }

    /**
     * Create a ZipwhipClient that is ready to go. You just have to call "Login" on it.
     * 
     * @return
     * @throws Exception
     */
    @Override
    public ZipwhipClient create() throws Exception {
        return new DefaultZipwhipClient(connectionFactory.create(), signalProviderFactory.create());
    }

}
