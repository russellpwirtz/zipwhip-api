package com.zipwhip.api.signals;

import com.ning.http.client.*;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.MutableObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.signals.presence.Presence;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * Date: 7/31/13
 * Time: 11:11 AM
 *
 * @author Michael
 * @version 1
 */
public class NingSignalsConnectActor implements SignalsConnectActor {

    private AsyncHttpClient client;
    private String url;
    private Executor executor;
    private Executor eventExecutor;

    @Override
    public ObservableFuture<Void> connect(String clientId, String sessionKey, String subscriptionId, Presence presence) {
        AsyncHttpClient.BoundRequestBuilder builder = client.preparePut(url);

        builder.addParameter("clientId", clientId);
        builder.addParameter("session", sessionKey);
        builder.addParameter("subscriptionId", subscriptionId);

        MutableObservableFuture<Void> future = new DefaultObservableFuture<Void>(this, eventExecutor);

        try {
            builder.execute(new AsyncHandler<Object>() {
                @Override
                public void onThrowable(Throwable t) {

                }

                @Override
                public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                    return null;
                }

                @Override
                public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
                    return null;
                }

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                    return null;
                }

                @Override
                public Object onCompleted() throws Exception {
                    return null;
                }
            });
        } catch (IOException e) {
            future.setFailure(e);
        }

        return future;
    }

    @Override
    public ObservableFuture<Void> disconnect(String clientId, String sessionKey, String subscriptionId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
