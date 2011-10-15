package com.zipwhip.api;

import com.zipwhip.api.request.RequestBuilder;
import com.zipwhip.concurrent.DefaultNetworkFuture;
import com.zipwhip.concurrent.NetworkFuture;
import com.zipwhip.util.SignTool;
import com.zipwhip.util.DownloadURL;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.StringUtil;
import com.zipwhip.util.UrlUtil;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Provides a persistent connection to a User on Zipwhip.
 * <p/>
 * You initialize this class with a sessionKey and then can execute raw requests
 * on behalf of the user. If you want a more Object oriented way to interact
 * with Zipwhip, use Consumer instead of Connection.
 * <p/>
 * This class is thread safe.
 */
public class HttpConnection extends DestroyableBase implements ApiConnection {

    private static final Logger LOGGER = Logger.getLogger(HttpConnection.class);

    private String apiVersion = "/";
    private String host = DEFAULT_HOST;

    private String sessionKey;
    private SignTool authenticator;
    private ExecutorService bossExecutor = Executors.newCachedThreadPool();
    private ExecutorService workerExecutor = Executors.newSingleThreadExecutor();

    public HttpConnection() {
        super();
    }

    public HttpConnection(String apiKey, String secret) throws Exception {
        this(new SignTool(apiKey, secret));
    }

    public HttpConnection(SignTool authenticator) {
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

    @Override
    public NetworkFuture<String> send(String method, Map<String, Object> params) {

        RequestBuilder rb = new RequestBuilder();

        // convert the map into a key/value HTTP params string
        rb.params(params, true);

        return send(method, rb.build());
    }

    private NetworkFuture<String> send(final String method, final String params) {

        // NOTE: if this is a SimpleExecutor (single threaded) then this will be a deadlock.
        final NetworkFuture<String> future = new DefaultNetworkFuture<String>(this, workerExecutor);

        bossExecutor.execute(new Runnable() {
            @Override
            public void run() {

                String result;

                try {
                    result = DownloadURL.get(UrlUtil.getSignedUrl(host, apiVersion, method, params, sessionKey, authenticator));
                } catch (Exception e) {

                    LOGGER.fatal("problem with DownloadUrl", e);

                    // NOTE: if this is a SimpleExecutor (single threaded) then this will be a deadlock. (workerExecutor)
                    future.setFailure(e);
                    return;
                }

                // NOTE: if this is a SimpleExecutor (single threaded) then this will be a deadlock. (workerExecutor)
                future.setSuccess(result);
            }
        });

        return future;
    }

    @Override
    protected void onDestroy() {
        bossExecutor.shutdownNow();
        workerExecutor.shutdownNow();
    }

}
