package com.zipwhip.vendor;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ApiConnectionFactory;
import com.zipwhip.util.Factory;

/**
 * This factory produces {@code AsyncVendorClient}s that are authenticated.
 */
public class AsyncVendorClientFactory implements Factory<AsyncVendorClient> {

    private static final String API_VERSION = "/vendor/v1/";

    private Factory<ApiConnection> connectionFactory;

    private AsyncVendorClientFactory(ApiConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Create a new AsyncVendorClient which has been authenticated via apiKey.
     *
     * By default this method will be over HTTPS.
     *
     * @param apiKey The Zipwhip assigned, vendor specific key.
     * @param secret The Zipwhip assigned, vendor specific secret.
     * @return An authenticated {@link AsyncVendorClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static AsyncVendorClient createViaApiKey(String apiKey, String secret) throws Exception {

        ApiConnectionFactory connectionFactory = ApiConnectionFactory.newAsyncHttpsInstance().apiKey(apiKey).secret(secret).apiVersion(API_VERSION);

        AsyncVendorClientFactory asyncVendorClientFactory = new AsyncVendorClientFactory(connectionFactory);

        return asyncVendorClientFactory.create();
    }

    /**
     * Create a new AsyncVendorClient which has been authenticated via apiKey.
     *
     * By default this method will be over HTTPS.
     *
     * @param apiKey The Zipwhip assigned, vendor specific key.
     * @param secret The Zipwhip assigned, vendor specific secret.
     * @param host The host to connect to.
     * @return An authenticated {@link AsyncVendorClient}
     * @throws Exception if an error occurs creating or authenticating the client.
     */
    public static AsyncVendorClient createViaApiKey(String apiKey, String secret, String host) throws Exception {

        ApiConnectionFactory connectionFactory = ApiConnectionFactory.newAsyncHttpsInstance().apiKey(apiKey).secret(secret).apiVersion(API_VERSION).host(host);

        AsyncVendorClientFactory asyncVendorClientFactory = new AsyncVendorClientFactory(connectionFactory);

        return asyncVendorClientFactory.create();
    }

    /**
     * Create an authenticated AsyncVendorClient.
     *
     * @return An authenticated AsyncVendorClient.
     */
    @Override
    public AsyncVendorClient create() {
        return new DefaultAsyncVendorClient(connectionFactory.create());
    }

}
