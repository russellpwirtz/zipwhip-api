package com.zipwhip.api;

import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.SocketSignalProviderFactory;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.util.Factory;

/**
 * This factory produces {@code ZipwhipClient}s that are authenticated.
 */
public class ZipwhipClientFactory implements Factory<ZipwhipClient> {

    private Factory<ApiConnection> connectionFactory;
    private Factory<SignalProvider> signalProviderFactory;
    private ImportantTaskExecutor importantTaskExecutor;

    public ZipwhipClientFactory() {
        this(null, null, null);
    }

    public ZipwhipClientFactory(ApiConnectionFactory connectionFactory, SocketSignalProviderFactory signalProviderFactory) {
        this(connectionFactory, signalProviderFactory, null);
    }

    public ZipwhipClientFactory(ApiConnectionFactory connectionFactory, SocketSignalProviderFactory signalProviderFactory, ImportantTaskExecutor importantTaskExecutor) {
        this.connectionFactory = connectionFactory;
        this.signalProviderFactory = signalProviderFactory;
        this.importantTaskExecutor = importantTaskExecutor;

        if (this.importantTaskExecutor == null){
            this.importantTaskExecutor = new ImportantTaskExecutor();
        }
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
     * Create a new ZipwhipClient which has been authenticated via a username and password.
     *
     * The Connection will use {@code NingHttpConnection}
     *
     * @param username The mobile number of the user.
     * @param password The user's Zipwhip password.
     * @return An authenticated {@link ZipwhipClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static ZipwhipClient createAsyncViaUsername(String username, String password) throws Exception {

        ApiConnectionFactory connectionFactory = ApiConnectionFactory.newAsyncInstance().username(username).password(password);
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
     * Create a new ZipwhipClient which has been pre-authenticated via the sessionKey.
     *
     * The Connection will use {@code NingHttpConnection}
     *
     * @param sessionKey A valid Zipwhip sessionKey.
     * @return An authenticated {@link ZipwhipClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static ZipwhipClient createAsyncViaSessionKey(String sessionKey) throws Exception {

        ApiConnectionFactory connectionFactory = ApiConnectionFactory.newAsyncInstance().sessionKey(sessionKey);
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
        DefaultZipwhipClient client = new DefaultZipwhipClient(null, connectionFactory.create(), signalProviderFactory.create());

        // this guy will do our /signals/connect calls with cancellation and timeout support.
        client.setImportantTaskExecutor(importantTaskExecutor);

        return client;
    }

}
