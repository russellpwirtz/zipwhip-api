package com.zipwhip.api;

import com.zipwhip.api.connection.Connection;
import com.zipwhip.api.connection.ParameterizedRequest;
import com.zipwhip.api.connection.RequestBody;
import com.zipwhip.api.connection.RequestMethod;
import com.zipwhip.api.dto.EnrollmentResult;
import com.zipwhip.api.response.JsonResponseParser;
import com.zipwhip.api.response.ResponseParser;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.response.StringServerResponse;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ExecutorFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Converter;
import com.zipwhip.util.InputRunnable;
import com.zipwhip.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A base class for future implementation to extend.
 * <p/>
 * It takes all the non-API specific stuff out of ZipwhipClient implementations.
 * <p/>
 * If some class wants to communicate with Zipwhip, then it needs to extend this
 * class. This class gives functionality that can be used to parse Zipwhip API.
 * This naming convention was copied from Spring (JmsSupport) base class.
 */
public abstract class ZipwhipNetworkSupport extends CascadingDestroyableBase {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ZipwhipNetworkSupport.class);

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * The default timeout when connecting to Zipwhip
     */
    public static final long DEFAULT_TIMEOUT_SECONDS = 45;

    public static final String SESSION_GET = "session/get";
    public static final String PRESENCE_GET = "presence/get";

    public static final String CONTACT_LIST = "contact/list";
    public static final String CONTACT_DELETE = "contact/delete";
    public static final String CONTACT_SAVE = "contact/save";
    public static final String CONTACT_GET = "contact/get";

    public static final String MESSAGE_SEND = "message/send";
    public static final String MESSAGE_LIST = "message/list";
    public static final String MESSAGE_READ = "message/read";
    public static final String MESSAGE_DELETE = "message/delete";
    public static final String MESSAGE_GET = "message/get";

    public static final String CONVERSATION_LIST = "conversation/list";
    public static final String CONVERSATION_READ = "conversation/read";
    public static final String CONVERSATION_DELETE = "conversation/delete";
    public static final String CONVERSATION_GET = "conversation/get";

    public static final String DEVICE_SAVE = "device/save";
    public static final String DEVICE_GET = "device/get";
    public static final String DEVICE_LIST = "device/list";
    public static final String DEVICE_DELETE = "device/delete";

    public static final String GROUP_SAVE = "group/save";
    public static final String GROUP_ADD_MEMBER = "group/addMember";

    public static final String SIGNALS_DISCONNECT = "signals/disconnect";
    public static final String SIGNALS_CONNECT = "signals/connect";
    public static final String SIGNALS_VERIFY = "signal/verify";
    public static final String SIGNAL_SEND = "signal/send";

    public static final String USER_ENROLL = "user/enroll";
    public static final String USER_DEACT = "user/deact";
    public static final String USER_SAVE = "user/save";
    public static final String USER_GET = "user/get";
    public static final String USER_EXISTS = "user/exists";
    public static final String USER_UNENROLL = "user/unenroll";

    public static final String CARBON_ENABLE = "v1/carbon/enable";
    public static final String CARBON_ENABLED = "v1/carbon/enabled";
    public static final String CARBON_ENABLED_VENDOR = "carbon/enabled";
    public static final String CARBON_INSTALLED = "carbon/installed";
    public static final String CARBON_SUGGEST = "carbon/suggest";
    public static final String CARBON_REGISTER = "carbon/register";
    public static final String CARBON_V2_REGISTER = "carbon/v2/register";
    public static final String CARBON_STATS = "carbon/stats";
    public static final String CARBON_ACCEPTED_TCS = "carbon/acceptedTCs";

    public static final String CHALLENGE_REQUEST = "session/v2/challenge";
    public static final String CHALLENGE_CONFIRM = "session/challenge/confirm";

    public static final String TEXTLINE_PROVISION = "textline/provision";
    public static final String TEXTLINE_ENROLL = "textline/enroll";
    public static final String TEXTLINE_UNENROLL = "textline/unenroll";

    public static final String FACE_IMAGE = "face/image";
    public static final String FACE_NAME = "face/name";

    public static final String ATTACHMENT_LIST = "messageAttachment/list";
    public static final String HOSTED_CONTENT_GET = "hostedContent/get";
    public static final String HOSTED_CONTENT_SAVE = "hostedContent/save";

    public static final String TINY_URL_RESERVE = "tinyUrl/reserve";
    public static final String TINY_URL_SAVE = "tinyUrl/save";

    /**
     * A runnable for for for executing asynchronous server responses.
     */
    private static final InputRunnable<ParsableServerResponse<ServerResponse>> FORWARD_RUNNABLE = new InputRunnable<ParsableServerResponse<ServerResponse>>() {
        @Override
        public void run(ParsableServerResponse<ServerResponse> object) {
            object.getFuture().setSuccess(object.getServerResponse());
        }
    };

    protected final Converter<InputStream, ServerResponse> normalStringResponseConverter = new Converter<InputStream, ServerResponse>() {
        @Override
        public ServerResponse convert(InputStream inputStream) throws Exception {
            String body = StreamUtil.getString(inputStream);

            return responseParser.parse(body);
        }

        @Override
        public InputStream restore(ServerResponse serverResponse) throws Exception {
            return null;
        }
    };

    protected final Converter<InputStream, ServerResponse> nullStreamConverter = new Converter<InputStream, ServerResponse>() {
        @Override
        public ServerResponse convert(InputStream inputStream) throws Exception {
            return null;
        }

        @Override
        public InputStream restore(ServerResponse serverResponse) throws Exception {
            return null;
        }
    };

    /**
     * This importantTaskExecutor really matters. This is the importantTaskExecutor that runs client code. I mean, the guys that call us.
     * They are observing our web calls via this importantTaskExecutor. If it's too small, and they are too slow, it'll backlog.
     */
    protected final Executor callbackExecutor;

    protected Connection connection;
    protected ResponseParser responseParser;

    /**
     * Create a new default {@code ZipwhipNetworkSupport}
     */
    public ZipwhipNetworkSupport() {
        this(null);
    }

    public ZipwhipNetworkSupport(ApiConnection connection) {
        this(null, connection);
    }

    public ZipwhipNetworkSupport(Executor callbackExecutor, Connection connection) {
        if (callbackExecutor == null) {
            callbackExecutor = ExecutorFactory.newInstance("ZipwhipNetworkSupport-callbacks");
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ((ExecutorService) ZipwhipNetworkSupport.this.callbackExecutor).shutdownNow();
                }
            });
        }
        this.callbackExecutor = callbackExecutor;

        setConnection(connection);
        link(connection);

        setResponseParser(new JsonResponseParser());
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        if (this.connection != null) {
            unlink(this.connection);
        }
        this.connection = connection;
        link(this.connection);
    }

    public ResponseParser getResponseParser() {
        return responseParser;
    }

    public void setResponseParser(ResponseParser responseParser) {
        this.responseParser = responseParser;
    }

    protected ObservableFuture<ServerResponse> executeAsync(String uri, Map<String, Object> params) throws Exception {
        return executeAsync(
                RequestMethod.GET,
                uri,
                new ParameterizedRequest(params),
                normalStringResponseConverter);
    }

    protected <T> ObservableFuture<T> executeAsync(RequestMethod method, String uri, RequestBody body, final Converter<InputStream, T> converter) throws Exception {
        final ObservableFuture<T> result = new DefaultObservableFuture<T>(this, callbackExecutor);

        getConnection()
                .send(method, uri, body)
                .addObserver(new Observer<ObservableFuture<InputStream>>() {

                    /**
                     * This code will execute in the "workerExecutor" of the connection.
                     * If you pass in a bogus/small importantTaskExecutor to him, our code will lag.
                     *
                     * @param sender The sender might not be the same object every time.
                     * @param item Rich object representing the notification.
                     */
                    @Override
                    public void notify(Object sender, ObservableFuture<InputStream> item) {
                        // The network is done! let's check for our cake!
                        if (!item.isDone()) {
                            return;
                        }

                        if (item.isCancelled()) {
                            // this will execute in the "callbackExecutor"
                            result.cancel();
                            return;
                        }

                        if (!item.isSuccess()) {
                            // this will execute in the "callbackExecutor"
                            result.setFailure(item.getCause());
                            return;
                        }

                        try {
                            result.setSuccess(converter.convert(item.getResult()));

                        } catch (Exception e) {
                            LOGGER.error("Problem parsing json response", e);
                            // this will execute in the "callbackExecutor"
                            result.setFailure(e);
                        }
                    }
                });

        return result;
    }

//    protected ObservableFuture<byte[]> executeAsyncBinaryResponse(String method, Map<String, Object> params, boolean requiresAuthentication) throws Exception {
//
//        if (requiresAuthentication && !connection.isAuthenticated()) {
//            throw new Exception("The connection is not authenticated, can't continue.");
//        }
//
//        final ObservableFuture<byte[]> result = new DefaultObservableFuture<byte[]>(this, callbackExecutor);
//
//        final ObservableFuture<InputStream> responseFuture = getConnection().sendBinaryResponse(method, params);
//
//        responseFuture.addObserver(new Observer<ObservableFuture<InputStream>>() {
//
//            @Override
//            public void notify(Object sender, ObservableFuture<InputStream> item) {
//
//                // The network is done! let's check for our cake!
//                if (!item.isDone()) {
//                    return;
//                }
//
//                if (item.isCancelled()) {
//                    // this will execute in the "callbackExecutor"
//                    result.cancel();
//                    return;
//                }
//                if (!item.isSuccess()) {
//                    // this will execute in the "callbackExecutor"
//                    result.setFailure(item.getCause());
//                    return;
//                }
//
//                byte[] bytes;
//
//                try {
//                    bytes = toByteArray(item.getResult());
//                } catch (IOException e) {
//                    result.setFailure(e);
//                    return;
//                }
//
//                result.setSuccess(bytes);
//            }
//        });
//
//        return result;
//    }

    protected ObservableFuture<Void> executeAsyncVoid(String uri, Map<String, Object> params) throws Exception {
        return toVoid(executeAsync(RequestMethod.GET, uri, new ParameterizedRequest(params), nullStreamConverter));
    }

    protected ObservableFuture<Void> executeAsyncVoid(RequestMethod method, String uri, Map<String, Object> params) throws Exception {
        return toVoid(executeAsync(method, uri, new ParameterizedRequest(params), nullStreamConverter));
    }

    protected ObservableFuture<Void> toVoid(ObservableFuture future) {
        final ObservableFuture<Void> result = new NestedObservableFuture<Void>(this);

        future.addObserver(new Observer<ObservableFuture>() {
            @Override
            public void notify(Object sender, ObservableFuture item) {
                NestedObservableFuture.syncState(item, result, null);
            }
        });

        return result;
    }

    protected void checkAndThrowError(ServerResponse serverResponse) throws Exception {
        if (serverResponse == null) {
            // A null response from the server is OK
            return;
        }

        if (!serverResponse.isSuccess()) {
            throwError(serverResponse);
        }
    }

    protected void throwError(ServerResponse serverResponse) throws Exception {
        if (serverResponse instanceof StringServerResponse) {
            StringServerResponse string = (StringServerResponse) serverResponse;

            throw new Exception(string.response);
        } else {
            throw new Exception(StreamUtil.getString(serverResponse.getRaw()));
        }
    }

    protected static byte[] toByteArray(InputStream input) throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        int numBytes;

        while (-1 != (numBytes = input.read(buffer))) {
            output.write(buffer, 0, numBytes);
        }

        return output.toByteArray();
    }

    protected <T> T get(ObservableFuture<T> task) throws Exception {

        task.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!task.isSuccess()) {
            throw new Exception(task.getCause());
        }

        return task.getResult();
    }

    protected boolean success(ServerResponse serverResponse) {
        return (serverResponse != null) && serverResponse.isSuccess();
    }

    protected static class ParsableServerResponse<T> {

        private ObservableFuture<T> future;
        private ServerResponse serverResponse;

        private ParsableServerResponse(ObservableFuture<T> future, ServerResponse serverResponse) {
            this.future = future;
            this.serverResponse = serverResponse;
        }

        public ObservableFuture<T> getFuture() {
            return future;
        }

        public ServerResponse getServerResponse() {
            return serverResponse;
        }
    }

}
