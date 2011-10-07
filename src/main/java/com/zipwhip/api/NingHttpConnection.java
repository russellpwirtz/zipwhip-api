package com.zipwhip.api;

//import com.ning.http.client.AsyncHttpClient;
//import com.ning.http.client.Response;
import com.zipwhip.api.request.RequestBuilder;
import com.zipwhip.util.SignTool;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.io.IOException;
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
public class NingHttpConnection extends DestroyableBase implements ApiConnection {

    public static final String DEFAULT_HOST = "http://network.zipwhip.com";

    private static final Logger LOGGER = Logger.getLogger(NingHttpConnection.class);

    private String apiVersion = "/";
    private String host = DEFAULT_HOST;

    private String sessionKey;
    private SignTool authenticator;

//    private AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    private ExecutorService executor = Executors.newCachedThreadPool();

    public NingHttpConnection() {
        super();
    }

    public NingHttpConnection(String apiKey, String secret) throws Exception {
        this(new SignTool(apiKey, secret));
    }

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

    @Override
    public Future<String> send(final String method, Map<String, Object> params) {

        final RequestBuilder rb = new RequestBuilder();

        // convert the map into a key/value HTTP params string
        rb.params(params);

        FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
//                return send(method, rb.build()).get().getResponseBody();
                return null;
            }
        });

        executor.execute(task);

        return task;
    }

//    private Future<Response> send(final String method, final String params) {
//
//        Future<Response> f = null;
//
//        try {
//            String url = getUrl(method);
//
//            f = asyncHttpClient.prepareGet(url + sign(method, params)).execute();
//
//        } catch (IOException e) {
//
//            LOGGER.error("Error calling method " + method, e);
//
//        } catch (Exception e) {
//
//            LOGGER.error("Error signing method " + method + " with params " + params, e);
//
//        }
//
//        return f;
//    }

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
//        asyncHttpClient.close();
        executor.shutdownNow();
    }

}

