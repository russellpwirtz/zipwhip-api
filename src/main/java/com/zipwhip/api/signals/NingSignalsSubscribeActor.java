package com.zipwhip.api.signals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ning.http.client.*;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.MutableObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.presence.UserAgent;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Date: 7/31/13
 * Time: 11:11 AM
 *
 * @author Michael
 * @version 1
 */
public class NingSignalsSubscribeActor implements SignalsSubscribeActor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NingSignalsSubscribeActor.class);

    private static final Gson GSON = new GsonBuilder()
            .create();

    private static final AsyncHandler<Void> HANDLER = new NullAsyncCompletionHandler<Void>();

    private AsyncHttpClient client;
    private String url;
    private Executor executor = SimpleExecutor.getInstance();
    private Executor eventExecutor = SimpleExecutor.getInstance();

    @Override
    public ObservableFuture<Void> subscribe(String clientId, String sessionKey, String subscriptionId, UserAgent userAgent) {
        AsyncHttpClient.BoundRequestBuilder builder = client.preparePost(url);

        builder.addParameter("clientId", clientId);
        builder.addParameter("session", sessionKey);
        builder.addParameter("subscriptionId", subscriptionId);
        builder.addParameter("userAgent", GSON.toJson(userAgent));

        MutableObservableFuture<Void> future = new DefaultObservableFuture<Void>(this, eventExecutor);

        return executeAsync(builder, HANDLER, future);
    }

    @Override
    public ObservableFuture<Void> unsubscribe(String clientId, String sessionKey, String subscriptionId) {
        throw new NotImplementedException();
    }

    public static Gson getGson() {
        return GSON;
    }

    public AsyncHttpClient getClient() {
        return client;
    }

    public void setClient(AsyncHttpClient client) {
        this.client = client;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    private <T> MutableObservableFuture<T> executeAsync(AsyncHttpClient.BoundRequestBuilder builder, AsyncHandler<T> handler, MutableObservableFuture<T> result) {
        try {
            ListenableFuture<T> future = builder.execute(handler);

            future.addListener(new ListenerRunnable<T>(future, result), executor);

            if (future.isDone()) {
                try {
                    result.setSuccess(future.get(5, TimeUnit.MINUTES));
                } catch (InterruptedException e) {
                    result.setFailure(e);
                } catch (ExecutionException e) {
                    result.setFailure(e);
                } catch (TimeoutException e) {
                    result.setFailure(e);
                }
            } else if (future.isCancelled()) {
                LOGGER.error("We're making the assumption that this cancellation (error) was propagated to the future via the handler.");
            }
        } catch (IOException e) {
            result.setFailure(e);
        }

        return result;
    }

//    private class ReflectionCompletionHandler<T> extends AsyncCompletionHandler<T> {
//
//        private final Class<T> clazz;
//        private final MutableObservableFuture<T> future;
//
//        private ReflectionCompletionHandler(Class<T> clazz, MutableObservableFuture<T> future) {
//            this.clazz = clazz;
//            this.future = future;
//        }
//
//        @Override
//        public T onCompleted(Response response) throws Exception {
//            switch (response.getStatusCode()) {
//                case 200:
//                    return GSON.fromJson(response.getResponseBody(), clazz);
//                default:
//                    throw new Exception(response.getStatusText());
//            }
//        }
//
//        @Override
//        public void onThrowable(Throwable t) {
//            super.onThrowable(t);
//
//            future.setFailure(t);
//        }
//    }

    private static class ListenerRunnable<T> implements Runnable {

        private final ListenableFuture<T> listenableFuture;
        private final MutableObservableFuture<T> observableFuture;

        private ListenerRunnable(ListenableFuture<T> listenableFuture, MutableObservableFuture<T> observableFuture) {
            this.listenableFuture = listenableFuture;
            this.observableFuture = observableFuture;
        }

        @Override
        public void run() {
            try {
                observableFuture.setSuccess(listenableFuture.get());
            } catch (Exception e) {
                observableFuture.setFailure(e);
            }
        }
    }

}
