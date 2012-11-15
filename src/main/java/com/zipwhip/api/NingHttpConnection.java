package com.zipwhip.api;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Part;
import com.ning.http.client.Response;
import com.ning.http.multipart.FilePart;
import com.zipwhip.api.connection.ApiConnectionBase;
import com.zipwhip.api.connection.RequestBody;
import com.zipwhip.api.connection.RequestMethod;
import com.zipwhip.api.request.RequestBuilder;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.util.Authenticator;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;
import com.zipwhip.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Provides a persistent connection to a User on Zipwhip.
 * <p/>
 * You initialize this class with a sessionKey or apiKey and then can execute raw requests
 * on behalf of the user. If you want a more Object oriented way to interact
 * with Zipwhip, use Consumer instead of Connection.
 * <p/>
 * This class is thread safe.
 */
public abstract class NingHttpConnection extends ApiConnectionBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(NingHttpConnection.class);

    private final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
//
//    /**
//     * @param method Each method has a name, example: user/get. See {@link ZipwhipNetworkSupport} for fields.
//     * @param params Map of query params to append to the method
//     * @param files  A list of Files to be added as parts for a multi part upload.
//     * @return NetworkFuture<String>  where the String result is the raw serer response.
//     */
//    @Override
//    public ObservableFuture<InputStream> send(RequestMethod method, String path, RequestBody body) throws Exception {
//        final RequestBuilder rb = new RequestBuilder();
//
//        // convert the map into a key/value HTTP params string
//        rb.params(params);
//
//        final ObservableFuture<String> responseFuture = new DefaultObservableFuture<String>(this, workerExecutor);
//
//        try {
//            com.ning.http.client.RequestBuilder builder = new com.ning.http.client.RequestBuilder();
//
//            /**
//             * The next 4 lines are needed because of a bug in Ning in NettyAsyncHttpProvider.java.
//             * Ning has not implemented multipart upload over SSH. If we are using HTTPS some files
//             * will result in a loop which can crash the JVM with an out of memory exception.
//             *
//             * https://issues.sonatype.org/browse/AHC-78
//             */
//            String toUseHost = host;
//
//            if (toUseHost.startsWith("https")) {
//                toUseHost = toUseHost.replaceFirst("https", "http");
//            }
//
//            builder.setUrl(UrlUtil.getSignedUrl(toUseHost, apiVersion, method, rb.build(), sessionKey, authenticator));
//
//            if (CollectionUtil.exists(files)) {
//
//                builder.setMethod("POST");
//
//                for (File file : files) {
//                    /**
//                     * TODO The first argument "data" is required for the TinyUrlController to work.
//                     * Unfortunately this breaks the HostedContentController if more than one file
//                     * is being uploaded. TinyUrlController needs to be fixed to get the file names
//                     * from the fileMap as HostedContentController does.
//                     */
//                    Part part = new FilePart("data", file, "multipart/form-data", null);
//                    builder.addBodyPart(part);
//                }
//            }
//
//            asyncHttpClient.prepareRequest(builder.build()).execute(new AsyncCompletionHandler<Object>() {
//
//                @Override
//                public Object onCompleted(Response response) throws Exception {
//
//                    try {
//                        // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
//                        responseFuture.setSuccess(response.getResponseBody());
//                    } catch (IOException e) {
//                        responseFuture.setFailure(e);
//                    }
//
//                    return response;
//                }
//
//                @Override
//                public void onThrowable(Throwable t) {
//                    responseFuture.setFailure(t);
//                }
//
//            }).addListener();
//
//        } catch (Exception e) {
//
//            LOGGER.error("Exception while hitting the web", e);
//
//            // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
//            responseFuture.setFailure(e);
//            return responseFuture;
//        }
//
//        return responseFuture;
//    }
//
//    @Override
//    public ObservableFuture<InputStream> sendBinaryResponse(String method, Map<String, Object> params) throws Exception {
//
//        final RequestBuilder rb = new RequestBuilder();
//
//        // convert the map into a key/value HTTP params string
//        rb.params(params);
//
//        final ObservableFuture<InputStream> responseFuture = new DefaultObservableFuture<InputStream>(this, workerExecutor);
//
//        try {
//            asyncHttpClient.prepareGet(UrlUtil.getSignedUrl(host, apiVersion, method, rb.build(), sessionKey, authenticator)).execute(new AsyncCompletionHandler<Object>() {
//
//                @Override
//                public Object onCompleted(Response response) throws Exception {
//
//                    // TODO Remove this once zipwhip uses real HTTP codes
//                    if (response.getContentType().contains("json")) {
//                        responseFuture.setFailure(new Exception("404 - Resource not found"));
//                        return response;
//                    }
//
//                    if (response.getStatusCode() >= 400) {
//                        responseFuture.setFailure(new Exception(response.getStatusText()));
//                        return response;
//                    }
//
//                    try {
//                        // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
//                        responseFuture.setSuccess(response.getResponseBodyAsStream());
//                    } catch (IOException e) {
//                        responseFuture.setFailure(e);
//                    }
//
//                    return response;
//                }
//
//                @Override
//                public void onThrowable(Throwable t) {
//                    responseFuture.setFailure(t);
//                }
//
//            });
//
//        } catch (Exception e) {
//
//            LOGGER.error("Exception while hitting the web", e);
//
//            // this will call the callbacks in the "workerExecutor" because of the constructor arg above.
//            responseFuture.setFailure(e);
//            return responseFuture;
//        }
//
//        return responseFuture;
//    }
//
//    @Override
//    protected void onDestroy() {
//        asyncHttpClient.close();
//    }

}

