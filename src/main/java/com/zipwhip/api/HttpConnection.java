package com.zipwhip.api;

import com.zipwhip.api.request.RequestBuilder;
import com.zipwhip.util.SignTool;
import com.zipwhip.util.DownloadURL;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.StringUtil;
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
    private ExecutorService executor = Executors.newCachedThreadPool();

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
        return StringUtil.exists(sessionKey);
    }

    @Override
    public boolean isConnected() {
        return StringUtil.exists(sessionKey);
    }

    @Override
    public Future<String> send(String method, Map<String, Object> params) {

        RequestBuilder rb = new RequestBuilder();

        // convert the map into a key/value HTTP params string
        rb.params(params);

        return send(method, rb.build());
    }

    private Future<String> send(final String method, final String params) {

        // put them together to form the full url
        FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {

                // this is the base url+api+method
                final String url = getUrl(method);

                // this is the query string part
                return DownloadURL.get(url + sign(method, params));
            }
        });

        // execute this webcall async
        executor.execute(task);

        return task;
    }

    private String sign(String method, String params) throws Exception {

        StringBuilder builder = new StringBuilder();
        builder.append(params);

        String connector = "&";

        if (StringUtil.isNullOrEmpty(params)) {
            connector = "?";
        }

        if (!StringUtil.isNullOrEmpty(sessionKey)) {
            builder.append(connector);
            builder.append("session=");
            builder.append(sessionKey);
            connector = "&";
        }

        builder.append(connector);
        builder.append("date=");
        builder.append(System.currentTimeMillis());

        String url = apiVersion + method + builder.toString();
        String signature = getSignature(url);

        if (signature != null && signature.length() != 0) {
            builder.append("&signature=");
            builder.append(signature);
        }

        url = host + apiVersion + method + builder.toString();
        LOGGER.debug("Signed url: " + url);

        return builder.toString();
    }

    private String getSignature(String url) throws Exception {

        if (this.authenticator == null) {
            return null;
        }

        String result = this.authenticator.sign(url);
        LOGGER.debug("Signing: " + url);

        return result;
    }

    private String getUrl(String method) {
        return host + apiVersion + method;
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
    }

}
