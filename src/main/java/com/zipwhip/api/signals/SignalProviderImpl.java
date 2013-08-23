package com.zipwhip.api.signals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.zipwhip.api.signals.dto.*;
import com.zipwhip.concurrent.*;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.signals.address.Address;
import com.zipwhip.signals.message.Message;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.UserAgent;
import com.zipwhip.util.FutureDateUtil;
import com.zipwhip.util.StringUtil;
import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Date: 7/24/13
 * Time: 5:28 PM
 *
 * SignalProvider is responsible for clientId. It is not responsible for executing a /signals/connect
 *
 * Because the caller needs to execute a /signals/connect (and then cancel/reset) we need to use the ConnectionHandle
 * metaphor.
 *
 * @author Michael
 * @version 1
 */
public class SignalProviderImpl extends CascadingDestroyableBase implements SignalProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignalProviderImpl.class);

    private final ObservableHelper<SubscribeResult> subscribeEvent;
    private final ObservableHelper<SubscribeResult> unsubscribeEvent;
    private final ObservableHelper<Throwable> exceptionEvent;
    private final ObservableHelper<Void> connectionChangedEvent;
    private final ObservableHelper<DeliveredMessage> messageReceivedEvent;

    private Executor executor = Executors.newSingleThreadExecutor();
    private Executor eventExecutor = SimpleExecutor.getInstance();
    private String url = "http://localhost:23123";
    private Presence presence = new Presence();
    private SignalsSubscribeActor signalsSubscribeActor;
    private ImportantTaskExecutor importantTaskExecutor = new ImportantTaskExecutor();

    private Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(DeliveredMessage.class, new DeliveredMessageTypeAdapter())
            .registerTypeHierarchyAdapter(Message.class, new MessageTypeAdapter())
            .registerTypeHierarchyAdapter(Address.class, new AddressTypeConverter())
            .registerTypeHierarchyAdapter(SubscribeCompleteContent.class, new SubscribeCompleteContentTypeAdapter())
            .create();

    private volatile SocketIO socketIO;
    private volatile MutableObservableFuture<Void> connectFuture;
    private volatile MutableObservableFuture<Void> disconnectFuture;
    private final Map<String, SubscriptionRequest> pendingSubscriptionRequests = new ConcurrentHashMap<String, SubscriptionRequest>();

    public SignalProviderImpl() {
        connectionChangedEvent = new ObservableHelper<Void>("ConnectionChangedEvent", eventExecutor);
        exceptionEvent = new ObservableHelper<Throwable>("ExceptionEvent", eventExecutor);
        subscribeEvent = new ObservableHelper<SubscribeResult>("SubscribeEvent", eventExecutor);
        unsubscribeEvent = new ObservableHelper<SubscribeResult>("UnsubscribeEvent", eventExecutor);
        messageReceivedEvent = new ObservableHelper<DeliveredMessage>("MessageReceivedEvent", eventExecutor);

        socketIO = new SocketIO();
        socketIO.setGson(gson);
    }

    @Override
    public ObservableFuture<Void> connect() throws IllegalStateException {
        return connect(getUserAgent(), getClientId());
    }

    @Override
    public ObservableFuture<Void> connect(UserAgent userAgent) throws IllegalStateException {
        return connect(userAgent, getClientId());
    }

    @Override
    public synchronized ObservableFuture<Void> connect(UserAgent userAgent, String clientId) throws IllegalStateException {
        if (connectFuture != null) {
            return connectFuture;
        }

        if (userAgent == null) {
            throw new IllegalStateException("The userAgent cannot be null");
        } else if (socketIO.isConnected()) {
            return fail("Already connected!");
        }

        // The only thing on Presence that a customer can specify is the userAgent.
        // Everything else is defined from the server.
        presence.setUserAgent(userAgent);

        MutableObservableFuture<Void> result = connectFuture = future();

        try {
            socketIO.connect(url, callback);
        } catch (MalformedURLException e) {
            connectFuture.setFailure(e);
            connectFuture = null;
        }

        return result;
    }

    @Override
    public ObservableFuture<Void> disconnect() {
        if (disconnectFuture != null) {
            return disconnectFuture;
        } else if (!socketIO.isConnected()) {
            // Already disconnected
            return new FakeObservableFuture<Void>(SignalProviderImpl.this, null);
        }

        // The socketIO callback will
        MutableObservableFuture<Void> result = disconnectFuture = future();

        socketIO.disconnect();

        return result;
    }

    @Override
    public synchronized ObservableFuture<SubscribeResult> subscribe(String sessionKey, String subscriptionId) {
        if (!socketIO.isConnected()) {
            return fail("Not connected");
        }

        String clientId = getClientId();
        if (StringUtil.isNullOrEmpty(clientId)) {
            return fail("ClientId not defined yet. Are you connected?");
        }

        if (StringUtil.isNullOrEmpty(subscriptionId)) {
            subscriptionId = sessionKey;
        }


        //
        // Make sure that any previous requests get cancelled.
        //
        SubscriptionRequest request = pendingSubscriptionRequests.remove(subscriptionId);
        if (request != null) {
            request.getFuture().cancel();
        }

        //
        // Nest the future within our result. The subscribe is a multi-step process. We need to make the webcall and then
        // wait until we receive a SubscriptionCompleteCommand.
        //
        return importantTaskExecutor.enqueue(executor,
                new SignalSubscribeCallback(sessionKey, subscriptionId), FutureDateUtil.in1Second());
    }

    @Override
    public ObservableFuture<Void> unsubscribe(final String sessionKey, final String subscriptionId) {
        if (!socketIO.isConnected()) {
            return new FakeFailingObservableFuture<Void>(this, new IllegalStateException("Not connected"));
        }

        return importantTaskExecutor.enqueue(executor,
                new SignalUnsubscribeCallback(sessionKey, subscriptionId), FutureDateUtil.in30Seconds());
    }

    private IOCallback callback = new IOCallback() {
        @Override
        public void onConnect() {
            synchronized (SignalProviderImpl.this) {
                if (connectFuture != null) {
                    connectFuture.setSuccess(null);
                    connectFuture = null;
                }
            }

            connectionChangedEvent.notifyObservers(SignalProviderImpl.this, null);
        }

        @Override
        public void onDisconnect() {
            synchronized (SignalProviderImpl.this) {
                if (disconnectFuture != null) {
                    disconnectFuture.setSuccess(null);
                    disconnectFuture = null;
                }
            }

            connectionChangedEvent.notifyObservers(SignalProviderImpl.this, null);
        }

        @Override
        public void onMessage(String message, IOAcknowledge ack) {
            // parse the message, detect the type, throw the appropriate event
        }

        @Override
        public void onSessionId(String sessionId) {

        }

        @Override
        public void onMessage(JsonElement element, IOAcknowledge ack) {
            // parse the message, detect the type, throw the appropriate event
            DeliveredMessage deliveredMessage = gson.fromJson(element, DeliveredMessage.class);
            Message message = deliveredMessage.getMessage();

            if (message == null) {
                LOGGER.error("Received a null message from " + element);
                return;
            }

            if (StringUtil.equalsIgnoreCase(message.getType(), "command")) {
                processCommand(message);
            } else if (StringUtil.equalsIgnoreCase(message.getType(), "subscribe")) {

                if (StringUtil.equalsIgnoreCase(message.getEvent(), "complete")) {
                    handleSubscribeComplete(deliveredMessage.getMessage());
                } else {
                    throw new IllegalStateException("Not sure what event this is: " + message.getEvent());
                }
            } else {
                messageReceivedEvent.notifyObservers(this, deliveredMessage);
            }
        }

        private void handleSubscribeComplete(Message message) {
            // the message is delivered directly to us.
            // Therefore the 'subscriptionIds' field will be null.
            SubscribeCompleteContent result = (SubscribeCompleteContent) message.getContent();
            SubscriptionRequest request = pendingSubscriptionRequests.remove(result.getSubscriptionId());
            MutableObservableFuture<SubscribeResult> future = request.getFuture();

            SubscribeResult subscribeResult = new SubscribeResult();

            subscribeResult.setSubscriptionId(result.getSubscriptionId());
            subscribeResult.setSessionKey(request.getSessionKey());
            subscribeResult.setChannels(result.getAddresses());

            future.setSuccess(subscribeResult);

            subscribeEvent.notifyObservers(SignalProviderImpl.this, subscribeResult);
        }

        @Override
        public void on(String s, IOAcknowledge ack, Object... objects) {
            // parse the message, detect the type, throw the appropriate event
        }

        @Override
        public void onError(SocketIOException e) {
            exceptionEvent.notifyObservers(SignalProviderImpl.this, e);
        }
    };

    private void processCommand(Message message) {

    }

    private <T> ObservableFuture<T> fail(Throwable throwable) {
        return new FakeFailingObservableFuture<T>(this, throwable);
    }

    private <T> ObservableFuture<T> fail(String message) {
        return fail(new IllegalStateException(message));
    }

    private <T> MutableObservableFuture<T> future() {
        return new DefaultObservableFuture<T>(this, eventExecutor);
    }

    @Override
    public synchronized ObservableFuture<Void> resetDisconnectAndConnect() {
        // TODO: protect threading (what happens if the connection cycles during this process)
        final MutableObservableFuture<Void> result = future();

        disconnect().addObserver(new Observer<ObservableFuture<Void>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Void> item) {
                if (item.isSuccess()) {
                    result.setSuccess(null);
                } else if (item.isCancelled()) {
                    result.cancel();
                } else if (item.isFailed()) {
                    result.setFailure(item.getCause());
                }

                throw new IllegalStateException("Not sure what state this is " + result);
            }
        });

        return result;
    }


    @Override
    public Observable<DeliveredMessage> getMessageReceivedEvent() {
        return messageReceivedEvent;
    }

    @Override
    public Observable<SubscribeResult> getSubscribeEvent() {
        return subscribeEvent;
    }

    @Override
    public UserAgent getUserAgent() {
        if (presence == null) {
            return null;
        }

        return presence.getUserAgent();
    }

    @Override
    public String getClientId() {
        return socketIO.getSessionId();
    }

    @Override
    public Observable<SubscribeResult> getUnsubscribeEvent() {
        return unsubscribeEvent;
    }

    @Override
    protected void onDestroy() {

    }

    public void setSignalsSubscribeActor(SignalsSubscribeActor signalsSubscribeActor) {
        this.signalsSubscribeActor = signalsSubscribeActor;
    }

    public SignalsSubscribeActor getSignalsSubscribeActor() {
        return signalsSubscribeActor;
    }

    @Override
    public boolean isConnected() {
        return socketIO.isConnected();
    }

    private class SignalUnsubscribeCallback implements Callable<ObservableFuture<Void>> {

        private final String sessionKey;
        private final String subscriptionId;

        private SignalUnsubscribeCallback(String sessionKey, String subscriptionId) {
            this.sessionKey = sessionKey;
            this.subscriptionId = subscriptionId;
        }

        @Override
        public ObservableFuture<Void> call() throws Exception {
            // NOTE: the connected state is not something that is able to stay constant.
            // For example, we just checked isConnected() but it might have changed a nanosecond after we checked it!

            // If the server hasn't fully processed your subscription (and sent a SubscriptionCompleteCommand) then
            // we're making a noop call here. It's ok. I don't mind the extra call to the web.
            ObservableFuture<Void> future = signalsSubscribeActor.unsubscribe(getClientId(), sessionKey, subscriptionId);

            // If disconnected successfully, remove from local list.
            future.addObserver(new Observer<ObservableFuture<Void>>() {
                @Override
                public void notify(Object sender, ObservableFuture<Void> item) {
                    if (item.isFailed()) {
                        subscribeEvent.notifyObservers(SignalProviderImpl.this, new SubscribeResult(sessionKey, subscriptionId, item.getCause()));
                        return;
                    }

                    if (item.isCancelled()) {
                        subscribeEvent.notifyObservers(SignalProviderImpl.this, new SubscribeResult(sessionKey, subscriptionId, new CancellationException()));
                        return;
                    }

                    subscribeEvent.notifyObservers(SignalProviderImpl.this, new SubscribeResult(sessionKey, subscriptionId));
                }
            });

            return future;
        }
    }

    private class SignalSubscribeCallback implements Callable<ObservableFuture<SubscribeResult>> {

        private final String sessionKey;
        private final String subscriptionId;

        private SignalSubscribeCallback(String sessionKey, String subscriptionId) {
            this.sessionKey = sessionKey;
            this.subscriptionId = subscriptionId;
        }

        @Override
        public ObservableFuture<SubscribeResult> call() throws Exception {
            MutableObservableFuture<SubscribeResult> result = future();
            final SubscriptionRequest request = new SubscriptionRequest(subscriptionId, sessionKey, result);

            // Make sure we're the only request that's processing.
            if (!addRequest(request)) {
                result.cancel();

                return result;
            }

            // Make the call to the server (async).
            ObservableFuture<Void> future = signalsSubscribeActor.subscribe(getClientId(), sessionKey, subscriptionId, getUserAgent());

            // We ONLY want to cascade the failure.
            // The success will come later when the SubscriptionCompleteCommand comes in.
            future.addObserver(new CascadeFailureObserver<Void>(result));

            // Clean up the pending request map.
            final String finalSubscriptionId = subscriptionId;
            future.addObserver(new Observer<ObservableFuture<Void>>() {
                @Override
                public void notify(Object sender, ObservableFuture<Void> item) {
                    if (item.isSuccess()) {
                        // We only want to clean up after failed requests.
                        // Otherwise, the SubscriptionComplete will clean up later.
                        return;
                    }

                    synchronized (SignalProviderImpl.this) {
                        SubscriptionRequest request1 = pendingSubscriptionRequests.get(finalSubscriptionId);

                        // Only remove if it's the same one.
                        if (request1 == request) {
                            pendingSubscriptionRequests.remove(finalSubscriptionId);

                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(String.format("Removed \"%s\" from pendingSubscriptionRequests", finalSubscriptionId));
                            }
                        }
                    }
                }
            });

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Finished requesting /signal/subscribe. Now we're waiting for a SubscriptionComplete to come down the wire!");
            }

            return result;
        }

        private boolean addRequest(SubscriptionRequest request) {
            //
            // Make sure we're the only request that's being processed right now.
            //
            synchronized (SignalProviderImpl.this) {
                SubscriptionRequest existingSubscriptionRequest = pendingSubscriptionRequests.get(subscriptionId);

                if (existingSubscriptionRequest != null) {
                    // Something is already cooking. Cancel ours.
                    request.getFuture().cancel();

                    return false;
                }

                pendingSubscriptionRequests.put(subscriptionId, request);
            }

            return true;
        }
    }

    private static class CascadeFailureObserver<T> implements Observer<ObservableFuture<T>> {

        private final MutableObservableFuture result;

        public CascadeFailureObserver(MutableObservableFuture result) {
            this.result = result;
        }

        @Override
        public void notify(Object sender, ObservableFuture<T> item) {
            if (item.isFailed()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Sync failure from %s to %s", item, result));
                }

                NestedObservableFuture.syncFailure(item, result);
            }
        }
    }
}
