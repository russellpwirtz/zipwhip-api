package com.zipwhip.api.signals;

import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.FakeFailingObservableFuture;
import com.zipwhip.concurrent.NestedObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.signals.message.Message;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.timers.Timer;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;
import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.plugin.dom.exception.InvalidStateException;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

    private final ObservableHelper<SubscriptionResult> bindEvent;
    private final ObservableHelper<Throwable> exceptionEvent;
    private final ObservableHelper<Void> connectionChangedEvent;
    private final ObservableHelper<Void> newClientIdReceivedEvent;
    private final ObservableHelper<Message> messageReceivedEvent;

    private Executor executor = Executors.newSingleThreadExecutor();
    private Executor eventExecutor = SimpleExecutor.getInstance();
    private String clientId;
    private String url;
    private Presence presence;
    private SignalsConnectActor signalsConnectActor;
    private Timer timer;

    private final Set<Subscription> subscriptions = new HashSet<Subscription>();

    private volatile SocketIO socketIO;
    private volatile DefaultObservableFuture<Void> connectFuture;
    private volatile DefaultObservableFuture<Void> disconnectFuture;

    public SignalProviderImpl() {
        connectionChangedEvent = new ObservableHelper<Void>("ConnectionChangedEvent", eventExecutor);
        exceptionEvent = new ObservableHelper<Throwable>("ExceptionEvent", eventExecutor);
        bindEvent = new ObservableHelper<SubscriptionResult>("BindEvent", eventExecutor);
        messageReceivedEvent = new ObservableHelper<Message>("MessageReceivedEvent", eventExecutor);
        newClientIdReceivedEvent = new ObservableHelper<Void>("NewClientIdReceivedEvent", eventExecutor);
    }

    @Override
    public synchronized ObservableFuture<Void> connect() {
        if (presence == null) {
            return new FakeFailingObservableFuture<Void>(this, new InvalidStateException("Presence"));
        }

        if (connectFuture != null) {
            return connectFuture;
        }

        DefaultObservableFuture<Void> result = connectFuture = new DefaultObservableFuture<Void>(this, eventExecutor);

        try {
            socketIO.connect(url, callback);
        } catch (MalformedURLException e) {
            connectFuture.setFailure(e);
            connectFuture = null;
        }

        return result;
    }

    @Override
    public synchronized ObservableFuture<Void> disconnect(boolean causedByNetwork) {
        if (disconnectFuture != null) {
            return disconnectFuture;
        }

        DefaultObservableFuture<Void> result = disconnectFuture = new DefaultObservableFuture<Void>(this, eventExecutor);

        socketIO.disconnect();

        return result;
    }

    @Override
    public ObservableFuture<SubscriptionCompleteCommand> bind(String sessionKey, String subscriptionId) {
        if (!socketIO.isConnected()) {
            return new FakeFailingObservableFuture<SubscriptionCompleteCommand>(this, new IllegalStateException("Not connected"));
        }

        ObservableFuture<Void> future = signalsConnectActor.connect(clientId, sessionKey, subscriptionId, presence);
        DefaultObservableFuture<SubscriptionCompleteCommand> result = new DefaultObservableFuture<SubscriptionCompleteCommand>(this, eventExecutor);

        // if this future fails, cascade to the recipient.
        future.addObserver(new CascadeFailureObserver<Void>(result));

        // we need to wait for a SubscriptionCompleteCommand to come in as a message.

        return result;
    }

    @Override
    public ObservableFuture<Void> unbind(String subscriptionId) {
        if (!socketIO.isConnected()) {
            return new FakeFailingObservableFuture<Void>(this, new IllegalStateException("Not connected"));
        }

        // NOTE: the connected state is not something that is able to stay constant.
        // For example, we just checked isConnected() but it might have changed a nanosecond after we checked it!

        final Subscription subscription = findSubscriptionBySubscriptionId(subscriptionId);

        if (subscription == null) {
            return new FakeFailingObservableFuture<Void>(this, new IllegalStateException("Not found"));
        }

        // cancel the old subscription future (if one was pending)
        cancelSubscriptionCompleteFuture(subscription);

        // If the server hasn't fully processed your subscription (and sent a SubscriptionCompleteCommand) then
        // we're making a noop call here. It's ok. I don't mind the extra call to the web.
        ObservableFuture<Void> future = signalsConnectActor.disconnect(clientId, subscription.getSessionKey(), subscriptionId);

        // If disconnected successfully, remove from local list.
        future.addObserver(new Observer<ObservableFuture<Void>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Void> item) {
                if (item.isSuccess()) {
                    subscriptions.remove(subscription);

                    bindEvent.notifyObservers(SignalProviderImpl.this, new SubscriptionResult(true, false, subscription));
                }
            }
        });

        return null;
    }

    private Subscription findSubscriptionBySubscriptionId(String subscriptionId) {
        if (CollectionUtil.isNullOrEmpty(subscriptions)) {
            return null;
        }

        for (Subscription subscription : subscriptions) {
            if (subscription.getSubscriptionId().equals(subscriptionId)) {
                return subscription;
            }
        }

        return null;
    }

    private void cancelSubscriptionCompleteFuture(Subscription subscription) {
        if (subscription.getInnerConnectFuture() != null) {
            ObservableFuture<?> future1 = subscription.getInnerConnectFuture();
            future1.cancel();
            subscription.setInnerConnectFuture(null);
        }

        if (subscription.getSubscriptionCompleteFuture() != null) {
            ObservableFuture<?> future1 = subscription.getSubscriptionCompleteFuture();
            future1.cancel();
            subscription.setSubscriptionCompleteFuture(null);
        }
    }

    private IOCallback callback = new IOCallback() {
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
        public void onMessage(String message, IOAcknowledge ack) {
            // parse the message, detect the type, throw the appropriate event
        }

        @Override
        public void onMessage(JSONObject object, IOAcknowledge ack) {
            // parse the message, detect the type, throw the appropriate event
            Message message = parseMessage(object);

            if (message == null) {
                LOGGER.error("Received a null message from " + object);
                return;
            }

            if (StringUtil.equalsIgnoreCase(message.getType(), "command")) {
                processCommand(message);
            } else {
                messageReceivedEvent.notifyObservers(this, message);
            }
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

    private Message parseMessage(JSONObject object) {
        return null;
    }

    @Override
    public ObservableFuture<Void> disconnect() {
        return disconnect(false);
    }

    @Override
    public synchronized ObservableFuture<Void> resetDisconnectAndConnect() {
        // TODO: protect threading (what happens if the connection cycles during this process)
        final DefaultObservableFuture<Void> result = new DefaultObservableFuture<Void>(this, eventExecutor);

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
    public Observable<Message> getMessageReceivedEvent() {
        return messageReceivedEvent;
    }

    @Override
    public Observable<Void> getNewClientIdReceivedEvent() {
        return newClientIdReceivedEvent;
    }

    @Override
    public Observable<Throwable> getExceptionEvent() {
        return exceptionEvent;
    }

    @Override
    public Observable<SubscriptionResult> getBindEvent() {
        return bindEvent;
    }

    @Override
    public Presence getPresence() {
        return presence;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public Set<Subscription> getSubscriptions() {
        return subscriptions;
    }


    @Override
    protected void onDestroy() {

    }

    private static class CascadeFailureObserver<T> implements Observer<ObservableFuture<T>> {

        private final DefaultObservableFuture result;

        public CascadeFailureObserver(DefaultObservableFuture<SubscriptionCompleteCommand> result) {
            this.result = result;
        }

        @Override
        public void notify(Object sender, ObservableFuture<T> item) {
            NestedObservableFuture.syncFailure(item, result);
        }
    }
}
