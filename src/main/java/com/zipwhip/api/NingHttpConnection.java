package com.zipwhip.api;

import com.ning.http.client.*;
import com.ning.http.client.Part;
import com.ning.http.multipart.FilePart;
import com.zipwhip.api.request.RequestBuilder;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.SignTool;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.StringUtil;
import com.zipwhip.util.UrlUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
     * @param workerExecutor This importantTaskExecutor is what your code will execute in. Our recommendation is that it's large
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
     * @param workerExecutor This importantTaskExecutor is what your code will execute in. Our recommendation is that it's large
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
     * @param authenticator A {@code SignTool} to use for signing request URLs.
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
     * @param method Each method has a name, example: user/get. See {@link ZipwhipNetworkSupport} for fields.
     * @param params Map of query params to append to the method
     * @return NetworkFuture<String>  where the String result is the raw serer response.
     */
    @Override
    public ObservableFuture<String> send(final String method, Map<String, Object> params) throws Exception {
        return send(method, params, null);
    }

    /**
     * @param method Each method has a name, example: user/get. See {@link ZipwhipNetworkSupport} for fields.
     * @param params Map of query params to append to the method
     * @param files  A list of Files to be added as parts for a multi part upload.
     * @return NetworkFuture<String>  where the String result is the raw serer response.
     */
    @Override
    public ObservableFuture<String> send(String method, Map<String, Object> params, List<File> files) throws Exception {

        final RequestBuilder rb = new RequestBuilder();

        // convert the map into a key/value HTTP params string
        rb.params(params);

        final ObservableFuture<String> responseFuture = new DefaultObservableFuture<String>(this, workerExecutor);

        try {
            com.ning.http.client.RequestBuilder builder = new com.ning.http.client.RequestBuilder();

            /**
             * The next 4 lines are needed because of a bug in Ning in NettyAsyncHttpProvider.java.
             * Ning has not implemented multipart upload over SSH. If we are using HTTPS some files
             * will result in a loop which can crash the JVM with an out of memory exception.
             *
             * https://issues.sonatype.org/browse/AHC-78
             */
            String toUseHost = host;

            if (toUseHost.startsWith("https")) {
                toUseHost = toUseHost.replaceFirst("https", "http");
            }

            builder.setUrl(UrlUtil.getSignedUrl(toUseHost, apiVersion, method, rb.build(), sessionKey, authenticator));

            if (CollectionUtil.exists(files)) {

                builder.setMethod("POST");

                for (File file : files) {
                    /**
                     * TODO The first argument "data" is required for the TinyUrlController to work.
                     * Unfortunately this breaks the HostedContentController if more than one file
                     * is being uploaded. TinyUrlController needs to be fixed to get the file names
                     * from the fileMap as HostedContentController does.
                     */
                    Part part = new FilePart("data", file, "multipart/form-data", null);
                    builder.addBodyPart(part);
                }
            }

            asyncHttpClient.prepareRequest(builder.build()).execute(new AsyncCompletionHandler<Object>() {

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
    public ObservableFuture<InputStream> sendBinaryResponse(String method, Map<String, Object> params) throws Exception {

        final RequestBuilder rb = new RequestBuilder();

        // convert the map into a key/value HTTP params string
        rb.params(params);

        final ObservableFuture<InputStream> responseFuture = new DefaultObservableFuture<InputStream>(this, workerExecutor);

        try {
            asyncHttpClient.prepareGet(UrlUtil.getSignedUrl(host, apiVersion, method, rb.build(), sessionKey, authenticator)).execute(new AsyncCompletionHandler<Object>() {

                @Override
                public Object onCompleted(Response response) throws Exception {

                    // TODO Remove this once zipwhip uses real HTTP codes
                    if (response.getContentType().contains("json")) {
                        responseFuture.setFailure(new Exception("404 - Resource not found"));
                        return response;
                    }

                    if (response.getStatusCode() >= 400) {
                        responseFuture.setFailure(new Exception(response.getStatusText()));
                        return response;
                    }

                    try {
                        // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
                        responseFuture.setSuccess(response.getResponseBodyAsStream());
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

