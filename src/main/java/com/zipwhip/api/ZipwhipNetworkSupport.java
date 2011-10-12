package com.zipwhip.api;

import com.zipwhip.api.response.JsonResponseParser;
import com.zipwhip.api.response.ResponseParser;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.response.StringServerResponse;
import com.zipwhip.api.settings.PreferencesSettingsStore;
import com.zipwhip.api.settings.SettingsStore;
import com.zipwhip.api.settings.SettingsVersionStore;
import com.zipwhip.api.settings.VersionStore;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.concurrent.DefaultNetworkFuture;
import com.zipwhip.concurrent.NetworkFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.InputRunnable;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.*;

/**
 * A base class for future implementation to extend.
 * <p/>
 * It takes all the non-API specific stuff out of ZipwhipClient implementations.
 * <p/>
 * If some class wants to communicate with Zipwhip, then it needs to extend this
 * class. This class gives functionality that can be used to parse Zipwhip API.
 * This naming convention was copied from Spring (JmsSupport) base class.
 */
public abstract class ZipwhipNetworkSupport extends DestroyableBase {

    protected static final Logger LOGGER = Logger.getLogger(ZipwhipNetworkSupport.class);

    /**
     * The default timeout when connecting to Zipwhip
     */
    public static final long DEFAULT_TIMEOUT_SECONDS = 45;

    public static final String CONTACT_LIST = "contact/list";
    public static final String CONTACT_DELETE = "contact/delete";
    public static final String CONTACT_SAVE = "contact/save";
    public static final String CONTACT_GET = "contact/get";
    public static final String PRESENCE_GET = "presence/get";
    public static final String PHONE_LOOKUP = "phone/lookup";
    public static final String MESSAGE_SEND = "message/send";
    public static final String VENDOR_MESSAGE_SEND = "vendor/message/send";
    public static final String MESSAGE_LIST = "message/list";
    public static final String MESSAGE_READ = "message/read";
    public static final String MESSAGE_DELETE = "message/delete";
    public static final String MESSAGE_GET = "message/get";
    public static final String DEVICE_SAVE = "device/save";
    public static final String DEVICE_GET = "device/get";
    public static final String DEVICE_LIST = "device/list";
    public static final String GROUP_SAVE = "group/save";
    public static final String DEVICE_DELETE = "device/delete";
    public static final String USER_ENROLL = "user/enroll";
    public static final String SIGNALS_DISCONNECT = "signals/disconnect";
    public static final String SIGNALS_CONNECT = "signals/connect";
    public static final String SIGNAL_SEND = "signal/send";
    public static final String USER_SAVE = "user/save";
    public static final String GROUP_ADD_MEMBER = "group/addMember";
    public static final String SESSION_GET = "session/get";
    public static final String CARBON_ENABLE = "v1/carbon/enable";
    public static final String CARBON_ENABLED = "v1/carbon/enabled";
    public static final String CHALLENGE_REQUEST = "session/challenge";
    public static final String CHALLENGE_CONFIRM = "session/challenge/confirm";

    /**
     * A runnable for for for executing asynchronous server responses.
     */
    private static final InputRunnable<ParsableServerResponse<ServerResponse>> FORWARD_RUNNABLE = new InputRunnable<ParsableServerResponse<ServerResponse>>() {
        @Override
        public void run(ParsableServerResponse<ServerResponse> object) {
            object.getFuture().setSuccess(object.getServerResponse());
        }
    };

    /**
     * This executor really matters. This is the executor that runs client code. I mean, the guys that call us.
     * They are observing our web calls via this executor. If it's too small, and they are too slow, it'll backlog.
     */
    private Executor callbackExecutor = Executors.newSingleThreadExecutor();

    protected ApiConnection connection;
    protected SignalProvider signalProvider;

    protected ResponseParser responseParser;
    protected SettingsStore settingsStore = new PreferencesSettingsStore();
    protected VersionStore versionsStore = new SettingsVersionStore(settingsStore);

    /**
     * Create a new default {@code ZipwhipNetworkSupport}
     */
    public ZipwhipNetworkSupport() {
        this(null, null);
    }

    public ZipwhipNetworkSupport(ApiConnection connection) {
        this(connection, null);
    }

    public ZipwhipNetworkSupport(SignalProvider signalProvider) {
        this(null, signalProvider);
    }

    public ZipwhipNetworkSupport(ApiConnection connection, SignalProvider signalProvider) {

        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }

        setConnection(connection);
        link(connection);

        if (signalProvider != null) {
            setSignalProvider(signalProvider);
            link(signalProvider);
        }

        setResponseParser(JsonResponseParser.getInstance());
    }

    public SignalProvider getSignalProvider() {
        return signalProvider;
    }

    public void setSignalProvider(SignalProvider signalProvider) {
        this.signalProvider = signalProvider;
    }

    public SettingsStore getSettingsStore() {
        return settingsStore;
    }

    public void setSettingsStore(SettingsStore store) {
        this.settingsStore = store;
        this.versionsStore = new SettingsVersionStore(store);
    }

    public ApiConnection getConnection() {
        return connection;
    }

    public void setConnection(ApiConnection connection) {
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

    protected ServerResponse executeSync(final String method, final Map<String, Object> params) throws Exception {
        return get(executeAsync(method, params, true, FORWARD_RUNNABLE));
    }

    protected ServerResponse executeSync(final String method, final Map<String, Object> params, boolean requiresAuthentication) throws Exception {
        return get(executeAsync(method, params, requiresAuthentication, FORWARD_RUNNABLE));
    }


    protected <T> NetworkFuture<T> executeAsync(String method, Map<String, Object> params, boolean requiresAuthentication, final InputRunnable<ParsableServerResponse<T>> businessLogic) throws Exception {

        if (requiresAuthentication && !connection.isAuthenticated()) {
            throw new Exception("The connection is not authenticated, can't continue.");
        }

        final NetworkFuture<T> result = new DefaultNetworkFuture<T>(callbackExecutor, this);

        final NetworkFuture<String> responseFuture = getConnection().send(method, params);

        responseFuture.addObserver(new Observer<NetworkFuture<String>>() {

            /**
             * This code will execute in the "workerExecutor" of the connection. If you pass in a bogus/small executor to him,
             * our code will lag.
             *
             * @param sender The sender might not be the same object every time, so we'll let it just be object, rather than generics.
             * @param item Rich object representing the notification.
             */
            @Override
            public void notify(Object sender, NetworkFuture<String> item) {
                // The network is done! let's check for our cake!
                if (!item.isDone()) {
                    return; // TODO: very weird, how did this happen?
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

                String responseString = item.getResult();

                ServerResponse serverResponse;
                try {
                    serverResponse = responseParser.parse(responseString);
                } catch (Exception e) {
                    LOGGER.fatal("Problem parsing json response", e);
                    // this will execute in the "callbackExecutor"
                    result.setFailure(e);
                    return;
                }

                try {
                    checkAndThrowError(serverResponse);
                } catch (Exception e) {
                    LOGGER.fatal("Server said failure", e);
                    // this will execute in the "callbackExecutor"
                    result.setFailure(e);
                    return;
                }

                try {
                    businessLogic.run(new ParsableServerResponse<T>(result, serverResponse));
                } catch (Exception e) {
                    LOGGER.fatal("Problem with running the business logic conversion", e);
                    // this will execute in the "callbackExecutor"
                    result.setFailure(e);
                }

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
            throw new Exception(serverResponse.getRaw());
        }
    }

    protected <T> T get(NetworkFuture<T> task) throws Exception {

        task.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!task.isSuccess()) {
            throw new Exception("exception for task", task.getCause());
        }

        return task.getResult();
    }

    protected boolean success(ServerResponse serverResponse) {
        return (serverResponse != null) && serverResponse.isSuccess();
    }

    protected static class ParsableServerResponse<T> {

        private NetworkFuture<T> future;
        private ServerResponse serverResponse;

        private ParsableServerResponse(NetworkFuture<T> future, ServerResponse serverResponse) {
            this.future = future;
            this.serverResponse = serverResponse;
        }

        public NetworkFuture<T> getFuture() {
            return future;
        }

        public ServerResponse getServerResponse() {
            return serverResponse;
        }
    }

}
