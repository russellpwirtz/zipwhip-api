package com.zipwhip.api;

import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.SocketSignalProviderFactory;
import com.zipwhip.util.Factory;

/**
 * This factory produces {@code ZipwhipClient}s that are authenticated.
 */
public class ZipwhipClientFactory implements Factory<ZipwhipClient> {

    private Factory<ApiConnection> connectionFactory;
    private Factory<SignalProvider> signalProviderFactory;

    public ZipwhipClientFactory() {

    }

    public ZipwhipClientFactory(ApiConnectionFactory connectionFactory, SocketSignalProviderFactory signalProviderFactory) {
        this.connectionFactory = connectionFactory;
        this.signalProviderFactory = signalProviderFactory;
    }

    /**
     * Create a new ZipwhipClient which has been authenticated via a username and password.
     *
     * @param username The mobile number of the user.
     * @param password The user's Zipwhip password.
     * @return An authenticated {@link ZipwhipClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static ZipwhipClient createViaUsername(String username, String password) throws Exception {

        ApiConnectionFactory connectionFactory = ApiConnectionFactory.newInstance().username(username).password(password);
        SocketSignalProviderFactory signalProviderFactory = SocketSignalProviderFactory.newInstance();

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, signalProviderFactory);

        return zipwhipClientFactory.create();
    }

    /**
     * Create a new ZipwhipClient which has been pre-authenticated via the sessionKey.
     *
     * @param sessionKey A valid Zipwhip sessionKey.
     * @return An authenticated {@link ZipwhipClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static ZipwhipClient createViaSessionKey(String sessionKey) throws Exception {

        ApiConnectionFactory connectionFactory = ApiConnectionFactory.newInstance().sessionKey(sessionKey);
        SocketSignalProviderFactory signalProviderFactory = SocketSignalProviderFactory.newInstance();

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, signalProviderFactory);

        return zipwhipClientFactory.create();
    }

    /**
     * Create an authenticated ZipwhipClient.
     * 
     * @return An authenticated ZipwhipClient.
     */
    @Override
    public ZipwhipClient create() {
        return new DefaultZipwhipClient(connectionFactory.create(), signalProviderFactory.create());
    }

}
