package com.zipwhip.api.connection;

import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.executors.CommonExecutorFactory;
import com.zipwhip.executors.CommonExecutorTypes;
import com.zipwhip.executors.DefaultCommonExecutorFactory;
import com.zipwhip.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

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

    private static final String BOUNDARY = "AaB03x";
    private static final String CRLF = "\r\n";

    private final ExecutorService executor;

    public HttpConnection(CommonExecutorFactory factory) {
        super();

        if (factory == null) {
            factory = DefaultCommonExecutorFactory.getInstance();
        }

        executor = factory.create(CommonExecutorTypes.WORKER, "HttpConnection");
    }

    public HttpConnection(String apiKey, String secret) throws Exception {
        this(null);

        this.setAuthenticator(new Authenticator(apiKey, secret));
    }

    public ObservableFuture<InputStream> send(final RequestMethod method, final String uri, final RequestBody body) throws Exception {
        final ObservableFuture<InputStream> future = new DefaultObservableFuture<InputStream>(this);

        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpURLConnection connection = openConnection(method, uri, body);

                        connection.connect();

                        try {
                            int statusCode = connection.getResponseCode();

                            if (300 < statusCode || statusCode < 200) {
                                InputStream stream = new BufferedInputStream(connection.getErrorStream());
                                String data = StreamUtil.getString(stream);

                                future.setFailure(new Exception("ResponseCode: " + statusCode + ": " + data));
                                return;
                            }

                            ByteArrayOutputStream stream = new ByteArrayOutputStream();

                            StreamUtil.copy(new BufferedInputStream(connection.getInputStream()), stream);

                            future.setSuccess(new ByteArrayInputStream(stream.toByteArray()));
                        } finally {
                            connection.disconnect();
                        }
                    } catch (Exception e) {
                        future.setFailure(e);
                    }
                }
            });


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
    private HttpURLConnection openConnection(RequestMethod method, String uri, RequestBody body) throws Exception {

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

        URL url = new URL(UrlUtil.getSignedUrl(toUseHost, getApiVersion(), uri, getUrlParams(method, body), getSessionKey(), getAuthenticator()));

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        if (isBodyAllowed(method)) {
            connection.setDoOutput(true);
        }

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setUseCaches(false);
        connection.setRequestMethod(getString(method));
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Connection", "Close");

        if (isBodyAllowed(method)) {
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            OutputStream outputStream = connection.getOutputStream();

            StreamUtil.copy(body.toStream(), outputStream);
        }

        return connection;
    }

//    private static class FutureAsyncCompletionHandler extends AsyncCompletionHandler<Object> {
//
//        private final ObservableFuture<InputStream> future;
//
//        private FutureAsyncCompletionHandler(ObservableFuture<InputStream> future) {
//            this.future = future;
//        }
//
//        @Override
//        public Object onCompleted(Response response) throws Exception {
//            try {
//                // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
//                if (response.getStatusCode() != 200) {
//                    future.setFailure(new Exception(String.format("HTTP Headers returned non-successful value: %s: %s", response.getStatusCode(), response.getStatusText())));
//                    return response;
//                }
//
//                future.setSuccess(response.getResponseBodyAsStream());
//            } catch (IOException e) {
//                future.setFailure(e);
//            }
//
//            return response;
//        }
//    }

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

        executor.shutdownNow();
    }
}
