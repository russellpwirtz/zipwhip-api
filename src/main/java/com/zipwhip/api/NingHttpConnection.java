package com.zipwhip.api;

import com.ning.http.client.*;
import com.zipwhip.api.request.RequestBuilder;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.util.SignTool;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.StringUtil;
import com.zipwhip.util.UrlUtil;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Provides a persistent connection to a User on Zipwhip.
 * <p/>
 * You initialize this class with a sessionKey or apiKey and then can execute raw requests
 * on behalf of the user. If you want a more Object oriented way to interact
 * with Zipwhip, use Consumer instead of Connection.
 * <p/>
 * This class is thread safe.
 */
public class NingHttpConnection extends DestroyableBase implements ApiConnection {

    private static final Logger LOGGER = Logger.getLogger(NingHttpConnection.class);

    private String apiVersion = DEFAULT_API_VERSION;
    private String host = DEFAULT_HOST;

    private String sessionKey;
    private SignTool authenticator;

    private AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    private Executor workerExecutor = Executors.newFixedThreadPool(10);

    /**
     * Create a new {@code NingHttpConnection} with a default configuration.
     */
    public NingHttpConnection() {
        super();
    }

    /**
     * Create a new {@code NingHttpConnection}
     *
     * @param workerExecutor This executor is what your code will execute in. Our recommendation is that it's large
     *                       because we have no idea how slow your code will be.
     * @param authenticator  A {@code SignTool} to use for signing request URLs.
     */
    public NingHttpConnection(Executor workerExecutor, SignTool authenticator) {
        this();
        this.workerExecutor = workerExecutor;
        this.authenticator = authenticator;
    }

    /**
     * Create a new {@code NingHttpConnection}
     *
     * @param workerExecutor This executor is what your code will execute in. Our recommendation is that it's large
     *                       because we have no idea how slow your code will be.
     */
    public NingHttpConnection(Executor workerExecutor) {
        this();
        if (workerExecutor != null) {
            this.workerExecutor = workerExecutor;
        }
    }

    /**
     * Create a new {@code NingHttpConnection}
     *
     * @param apiKey Used by a {@code SignTool} to sign request URLs.
     * @param secret Used by a {@code SignTool} to sign request URLs.
     * @throws Exception If an error is encountered creating the {@code SignTool}.
     */
    public NingHttpConnection(String apiKey, String secret) throws Exception {
        this(new SignTool(apiKey, secret));
    }

    /**
     * Create a new {@code NingHttpConnection}
     *
     * @param authenticator  A {@code SignTool} to use for signing request URLs.
     */
    public NingHttpConnection(SignTool authenticator) {
        this();
        this.setAuthenticator(authenticator);
    }

    @Override
    public void setAuthenticator(SignTool authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public SignTool getAuthenticator() {
        return this.authenticator;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public void setSessionKey(String sessionKey) {
        LOGGER.debug("Setting sessionKey to " + sessionKey);
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        LOGGER.debug("Getting sessionKey " + sessionKey);
        return sessionKey;
    }

    @Override
    public boolean isAuthenticated() {
        return StringUtil.exists(sessionKey) || (authenticator != null && authenticator.prepared());
    }

    @Override
    public boolean isConnected() {
        return isAuthenticated();
    }

    /**
     *
     * @param method Each method has a name, example: user/get. See {@link ZipwhipNetworkSupport} for fields.
     * @param params Map of query params to append to the method
     * @return NetworkFuture<String>  where the String result is the raw serer response.
     */
    @Override
    public ObservableFuture<String> send(final String method, Map<String, Object> params) {

        final RequestBuilder rb = new RequestBuilder();

        // convert the map into a key/value HTTP params string
        rb.params(params);

        final ObservableFuture<String> responseFuture = new DefaultObservableFuture<String>(this, workerExecutor);

        try {
            asyncHttpClient.prepareGet(UrlUtil.getSignedUrl(host, apiVersion, method, rb.build(), sessionKey, authenticator)).execute(new AsyncCompletionHandler<Object>() {

                @Override
                public Object onCompleted(Response response) throws Exception {

                    try {
                        // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
                        responseFuture.setSuccess(response.getResponseBody());
                    } catch (IOException e) {
                        responseFuture.setFailure(e);
                    }

                    return response;
                }

                @Override
                public void onThrowable(Throwable t) {
                    responseFuture.setFailure(t);
                }

            });

        } catch (Exception e) {

            LOGGER.error("Exception while hitting the web", e);

            // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
            responseFuture.setFailure(e);
            return responseFuture;
        }

        return responseFuture;
    }

    @Override
    protected void onDestroy() {
        asyncHttpClient.close();
    }

}

