package com.zipwhip.api.connection;

import com.ning.http.client.*;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import com.zipwhip.api.request.RequestBuilder;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ExecutorFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.executors.CommonExecutorFactory;
import com.zipwhip.executors.CommonExecutorTypes;
import com.zipwhip.executors.DefaultCommonExecutorFactory;
import com.zipwhip.executors.NamedThreadFactory;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides a persistent connection to a User on Zipwhip.
 * <p/>
 * You initialize this class with a sessionKey and then can execute raw requests
 * on behalf of the user. If you want a more Object oriented way to interact
 * with Zipwhip, use Consumer instead of Connection.
 * <p/>
 * This class is thread safe.
 */
public class HttpConnection extends ApiConnectionBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpConnection.class);

    private final AsyncHttpClient client;

    public HttpConnection(CommonExecutorFactory factory) {
        super();

        if (factory == null) {
            factory = DefaultCommonExecutorFactory.getInstance();
        }

        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
                .setExecutorService(factory.create(CommonExecutorTypes.WORKER, "HttpConnection"))
                .build();

        client = new AsyncHttpClient(config);
    }

    public HttpConnection(String apiKey, String secret) throws Exception {
        this(null);

        this.setAuthenticator(new Authenticator(apiKey, secret));
    }

    public ObservableFuture<InputStream> send(final RequestMethod method, final String uri, final RequestBody body) throws Exception {
        final ObservableFuture<InputStream> future = new DefaultObservableFuture<InputStream>(this);

        try {
            com.ning.http.client.RequestBuilder builder = createRequestBuilder(method, uri, body);

            // Ning is already asynchronous.
            client.prepareRequest(builder.build())
                    .execute(new FutureAsyncCompletionHandler(future));

        } catch (Exception e) {
            future.setFailure(e);
        }

        return future;
    }

    /**
     * Create a ning RequestBuilder off of the inputs.
     *
     * @param method
     * @param uri
     * @param body
     * @return
     * @throws Exception
     */
    private com.ning.http.client.RequestBuilder createRequestBuilder(RequestMethod method, String uri, RequestBody body) throws Exception {
        com.ning.http.client.RequestBuilder b = new com.ning.http.client.RequestBuilder();

        /**
         * The next 4 lines are needed because of a bug in Ning in NettyAsyncHttpProvider.java.
         * Ning has not implemented multipart upload over SSH. If we are using HTTPS some files
         * will result in a loop which can crash the JVM with an out of memory exception.
         *
         * https://issues.sonatype.org/browse/AHC-78
         */
        String toUseHost = getHost();

        // Removed to re-verify: Michael Nov 2012
//        if (toUseHost.startsWith("https")) {
//            toUseHost = toUseHost.replaceFirst("https", "http");
//        }
        ///////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////

        b.setMethod(getString(method));
        b.setUrl(UrlUtil.getSignedUrl(toUseHost, getApiVersion(), uri, getUrlParams(method, body), getSessionKey(), getAuthenticator()));

        if (isBodyAllowed(method)) {
            b.setBody(new InputStreamBodyGenerator(body.toStream()));
        }

        return b;
    }

    private static class FutureAsyncCompletionHandler extends AsyncCompletionHandler<Object> {

        private final ObservableFuture<InputStream> future;

        private FutureAsyncCompletionHandler(ObservableFuture<InputStream> future) {
            this.future = future;
        }

        @Override
        public Object onCompleted(Response response) throws Exception {
            try {
                // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
                if (response.getStatusCode() != 200) {
                    future.setFailure(new Exception(String.format("HTTP Headers returned non-successful value: %s: %s", response.getStatusCode(), response.getStatusText())));
                    return response;
                }

                future.setSuccess(response.getResponseBodyAsStream());
            } catch (IOException e) {
                future.setFailure(e);
            }

            return response;
        }
    }

    /**
     * If the method supports url based params, then return them. Otherwise just return
     *
     * @param method
     * @param body
     * @return
     * @throws IOException
     */
    private static String getUrlParams(RequestMethod method, RequestBody body) throws IOException {
        if (!isBodyAllowed(method)) {
            // since we cant put the parameters into the body, lets stick them on the url.
            return StreamUtil.getString(body.toStream());
        }

        return null;
    }

    private static boolean isBodyAllowed(RequestMethod method) {
        return !(method == RequestMethod.GET || method == RequestMethod.HEAD);
    }

    private static String getString(RequestMethod method) {
        return method.toString();
    }

    @Override
    protected void onDestroy() {
        LOGGER.debug("Destroying HttpConnection");

        client.closeAsynchronously();
    }
}
