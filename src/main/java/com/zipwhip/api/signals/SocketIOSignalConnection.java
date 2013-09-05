package com.zipwhip.api.signals;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.FakeObservableFuture;
import com.zipwhip.concurrent.MutableObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.util.FutureDateUtil;
import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Date: 9/5/13
 * Time: 3:28 PM
 *
 * @author Michael
 * @version 1
 */
public class SocketIOSignalConnection implements SignalConnection {

    private volatile SocketIO socketIO;
    private volatile ObservableFuture<Void> externalConnectFuture;
    private volatile MutableObservableFuture<Void> connectFuture;

    private final ObservableHelper<JsonElement> messageEvent;
    private final ObservableHelper<Void> disconnectEvent;
    private final ObservableHelper<Void> connectEvent;
    private final ObservableHelper<Throwable> exceptionEvent;

    private Executor eventExecutor = SimpleExecutor.getInstance();
    private Executor executor = SimpleExecutor.getInstance();

    private Gson gson;
    private ImportantTaskExecutor importantTaskExecutor;

    private String url;

    public SocketIOSignalConnection() {
        exceptionEvent = new ObservableHelper<Throwable>("ExceptionEvent", eventExecutor);
        connectEvent = new ObservableHelper<Void>("ConnectEvent", eventExecutor);
        disconnectEvent = new ObservableHelper<Void>("DisconnectEvent", eventExecutor);
        messageEvent = new ObservableHelper<JsonElement>("JsonMessageEvent", eventExecutor);
    }

    @Override
    public synchronized ObservableFuture<Void> connect() {
        if (externalConnectFuture != null) {
            return externalConnectFuture;
        }

        final ObservableFuture<Void> result = externalConnectFuture = importantTaskExecutor.enqueue(executor, new ConnectTask(), FutureDateUtil.in30Seconds());

        result.addObserver(new Observer<ObservableFuture<Void>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Void> item) {
                synchronized (SocketIOSignalConnection.this) {
                    connectFuture = null;
                    externalConnectFuture = null;
                }
            }
        });

        return result;
    }

    @Override
    public ObservableFuture<Void> disconnect() {
        socketIO.disconnect();
        socketIO = null;

        return new FakeObservableFuture<Void>(this, null);
    }

    private class ConnectTask implements Callable<ObservableFuture<Void>> {
        @Override
        public ObservableFuture<Void> call() throws Exception {
            synchronized (SocketIOSignalConnection.this) {
                connectFuture = new DefaultObservableFuture<Void>(this, eventExecutor);

                try {
                    socketIO = new SocketIO();
                    socketIO.setGson(gson);
                    socketIO.connect(url, callback);
                } catch (MalformedURLException e) {
                    connectFuture.setFailure(e);
                }

                return connectFuture;
            }
        }
    }

    private final IOCallback callback = new IOCallback() {
        @Override
        public void onDisconnect() {
            synchronized (SocketIOSignalConnection.this) {
                if (connectFuture != null) {
                    connectFuture.setFailure(new Exception("Disconnected"));
                }
            }

            disconnectEvent.notifyObservers(SocketIOSignalConnection.this, null);
        }

        @Override
        public void onConnect() {
            synchronized (SocketIOSignalConnection.this) {
                if (connectFuture != null) {
                    connectFuture.setSuccess(null);
                }
            }

            connectEvent.notifyObservers(SocketIOSignalConnection.this, null);
        }

        @Override
        public void onMessage(String data, IOAcknowledge ack) {
            onMessage(new JsonPrimitive(data), ack);
        }

        @Override
        public void onSessionId(String sessionId) {

        }

        @Override
        public void onMessage(JsonElement json, IOAcknowledge ack) {
            try {
                messageEvent.notifyObservers(SocketIOSignalConnection.this, json);
            } finally {
                ack.ack();
            }
        }

        @Override
        public void on(String event, IOAcknowledge ack, Object... args) {
            ack.ack();
        }

        @Override
        public void onError(SocketIOException socketIOException) {
            if (connectFuture != null) {
                connectFuture.setFailure(socketIOException);
            }

            exceptionEvent.notifyObservers(SocketIOSignalConnection.this, socketIOException);
        }
    };

    @Override
    public boolean isConnected() {
        if (socketIO == null) {
            return false;
        }

        return socketIO.isConnected();
    }

    @Override
    public ObservableFuture<ObservableFuture<Object[]>> emit(final String event, final Object... objects) {
        ObservableFuture<Object[]> ackFuture = importantTaskExecutor.enqueue(
                executor,
                new SendWithAckTask(socketIO, event, objects, eventExecutor),
                FutureDateUtil.in30Seconds());

        // the underlying library doesn't tell us when transmission is successful.
        // We have to just fake the "transmit" part of the future.

        return new FakeObservableFuture<ObservableFuture<Object[]>>(this, ackFuture);
    }

    private static class SendWithAckTask implements Callable<ObservableFuture<Object[]>> {

        private final SocketIO socketIO;
        private final String event;
        private final Object[] args;
        private final Executor eventExecutor;

        private SendWithAckTask(SocketIO socketIO, String event, Object[] args, Executor eventExecutor) {
            this.socketIO = socketIO;
            this.event = event;
            this.args = args;
            this.eventExecutor = eventExecutor;
        }

        @Override
        public ObservableFuture<Object[]> call() throws Exception {
            final MutableObservableFuture<Object[]> result = new DefaultObservableFuture<Object[]>(this, eventExecutor);
            socketIO.emit(event, new IOAcknowledge() {
                @Override
                public void ack(Object... args) {
                    result.setSuccess(args);
                }
            }, args);
            return result;
        }
    }

    @Override
    public Observable<Throwable> getExceptionEvent() {
        return exceptionEvent;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ImportantTaskExecutor getImportantTaskExecutor() {
        return importantTaskExecutor;
    }

    public void setImportantTaskExecutor(ImportantTaskExecutor importantTaskExecutor) {
        this.importantTaskExecutor = importantTaskExecutor;
    }

    public Gson getGson() {
        return gson;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Executor getEventExecutor() {
        return eventExecutor;
    }

    public void setEventExecutor(Executor eventExecutor) {
        this.eventExecutor = eventExecutor;
    }

    @Override
    public Observable<Void> getConnectEvent() {
        return connectEvent;
    }

    @Override
    public Observable<Void> getDisconnectEvent() {
        return disconnectEvent;
    }

    @Override
    public Observable<JsonElement> getMessageEvent() {
        return messageEvent;
    }
}
