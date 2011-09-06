package com.zipwhip.api;

import com.zipwhip.api.signals.SignalProvider;
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

    public ZipwhipClientFactory(HttpConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public ZipwhipClientFactory() {
    }

    public static ZipwhipClient createViaUsername(String username, String password) throws Exception {

        HttpConnectionFactory connectionFactory = HttpConnectionFactory.newInstance().username(username).password(password);

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory);

//        zipwhipClientFactory.setSignalProviderFactory(new SocketSignalProviderFactory());

        return zipwhipClientFactory.create();
    }

    public static ZipwhipClient createViaSessionKey(String sessionKey) throws Exception {

        HttpConnectionFactory connectionFactory = HttpConnectionFactory.newInstance().sessionKey(sessionKey);

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory);

//        zipwhipClientFactory.setSignalProviderFactory(new SocketSignalProviderFactory());

        return zipwhipClientFactory.create();
    }

    /**
     * Create a ZipwhipClient that is ready to go. You just have to call "Login"
     * on it.
     * 
     * @return
     * @throws Exception
     */
    @Override
    public ZipwhipClient create() throws Exception {

        DefaultZipwhipClient client = new DefaultZipwhipClient();

        client.setConnection(connectionFactory.create());

        setup(client);

        return client;
    }

//    public Factory<Connection> getConnectionFactory() {
//        return connectionFactory;
//    }
//
//    public void setConnectionFactory(Factory<Connection> connectionFactory) {
//        this.connectionFactory = connectionFactory;
//    }
//
//    public Factory<SignalProvider> getSignalProviderFactory() {
//        return signalProviderFactory;
//    }
//
//    public void setSignalProviderFactory(Factory<SignalProvider> signalProviderFactory) {
//        this.signalProviderFactory = signalProviderFactory;
//    }

    protected void setup(ZipwhipClient client) throws Exception {

        if (signalProviderFactory != null) {
            client.setSignalProvider(signalProviderFactory.create());
        }

        if (client.getConnection() != null && client.getSignalProvider() != null) {

            String sessionKey = client.getConnection().getSessionKey();

            if (StringUtil.exists(sessionKey)) {
                //client.getSignalProvider().(sessionKey);
            }
        }
    }
    
}
