package com.zipwhip.api;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import com.zipwhip.api.request.RequestBuilder;
import com.zipwhip.concurrent.DefaultNetworkFuture;
import com.zipwhip.concurrent.NetworkFuture;
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

    private Executor mainExecutor = Executors.newFixedThreadPool(10);
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
     * @param mainExecutor   This executor is used to retrieve HTTP responses. It defaults to a fixed 10 thread pool.
     *                       Our belief is that you'll never need it to be larger for any use case.
     *                       You generally should leave this to be null. If you set it as null, we'll execute it default. (10 thread pool)
     * @param workerExecutor This executor is what your code will execute in. Our recommendation is that it's large
     *                       because we have no idea how slow your code will be.
     * @param authenticator  A {@code SignTool} to use for signing request URLs.
     */
    public NingHttpConnection(Executor mainExecutor, Executor workerExecutor, SignTool authenticator) {
        this.mainExecutor = mainExecutor;
        this.workerExecutor = workerExecutor;
        this.authenticator = authenticator;
    }

    /**
     * Create a new {@code NingHttpConnection}
     *
     * @param workerExecutor This executor is what your code will execute in. Our recommendation is that it's large
     *                       because we have no idea how slow your code will be.
     * @param authenticator  A {@code SignTool} to use for signing request URLs.
     */
    public NingHttpConnection(Executor workerExecutor, SignTool authenticator) {
        this.workerExecutor = workerExecutor;
        this.authenticator = authenticator;
    }

    /**
     * Create a new {@code NingHttpConnection}
     *
     * @param mainExecutor   This executor is used to retrieve HTTP responses. It defaults to a fixed 10 thread pool.
     *                       Our belief is that you'll never need it to be larger for any use case.
     *                       You generally should leave this to be null. If you set it as null, we'll execute it default. (10 thread pool)
     * @param workerExecutor This executor is what your code will execute in. Our recommendation is that it's large
     *                       because we have no idea how slow your code will be.
     */
    public NingHttpConnection(Executor workerExecutor, Executor mainExecutor) {
        this(workerExecutor);

        if (mainExecutor != null) {
            this.mainExecutor = mainExecutor;
        }
    }

    /**
     * Create a new {@code NingHttpConnection}
     *
     * @param workerExecutor This executor is what your code will execute in. Our recommendation is that it's large
     *                       because we have no idea how slow your code will be.
     */
    public NingHttpConnection(Executor workerExecutor) {
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
        return StringUtil.exists(sessionKey);
    }

    @Override
    public boolean isConnected() {
        return StringUtil.exists(sessionKey);
    }

    /**
     * TODO: Unit test this and create java docs for it.
     *
     * @param method Each method has a name, example: user/get. See {@link ZipwhipNetworkSupport} for fields.
     * @param params Map of query params to append to the method
     * @return NetworkFuture<String>  where the String result is the raw serer response.
     */
    @Override
    public NetworkFuture<String> send(final String method, Map<String, Object> params) {

        final RequestBuilder rb = new RequestBuilder();

        // convert the map into a key/value HTTP params string
        rb.params(params);

        final NetworkFuture<String> responseFuture = new DefaultNetworkFuture<String>(workerExecutor);

        // TODO: Make sure this is NOT sync call.
        final ListenableFuture<Response> webCallFuture;

        try {
            webCallFuture = asyncHttpClient.prepareGet(UrlUtil.getSignedUrl(host, apiVersion, method, rb.build(), sessionKey, authenticator)).execute();
        } catch (Exception e) {
            LOGGER.fatal("Exception while hitting the web", e);
            // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
            responseFuture.setFailure(e);
            return responseFuture;
        }

        webCallFuture.addListener(new Runnable() {

            /**
             * This runnable will execute in the "mainExecutor" of this class. We are only doing quick operations
             * unless "workerExecutor" is synchronous. If that guy blocks, we block this executor.
             *
             * Because of the param below, this runnable executes in "mainExecutor"
             */
            @Override
            public void run() {

                if (!webCallFuture.isDone()) {
                    return;
                }

                if (webCallFuture.isCancelled()) {
                    // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
                    responseFuture.cancel();
                    return;
                }

                Response response;
                try {
                    // NOTE: this will not block because it's already done (our listener was fired)
                    response = webCallFuture.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
                    responseFuture.setFailure(e);
                    return;
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
                    responseFuture.setFailure(e);
                    return;
                }

                try {
                    // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
                    responseFuture.setSuccess(response.getResponseBody());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, mainExecutor);

        return responseFuture;
    }

    @Override
    protected void onDestroy() {
        asyncHttpClient.close();
    }

}

