package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.NestedObservableFuture;
import com.zipwhip.api.signals.Writable;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.util.Asserts;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: msmyers
 * Date: 9/4/12
 * Time: 4:48 PM
 *
 *
 */
public class SignalProviderConnectionHandle extends ConnectionHandleBase implements Writable {

    private static final Logger LOGGER = Logger.getLogger(SignalProviderConnectionHandle.class);

    private final SignalProviderBase signalProvider;
    public boolean finishedActionConnect = false;
    protected ConnectionHandle connectionHandle;

    public SignalProviderConnectionHandle(long id, SignalProviderBase signalProvider, ConnectionHandle connectionHandle) {
        super(id);
        this.signalProvider = signalProvider;
        this.connectionHandle = connectionHandle;
    }

    public SignalProviderConnectionHandle(long id, SignalProviderBase signalProvider) {
        super(id);
        this.signalProvider = signalProvider;
    }

    @Override
    public long getId() {
        return connectionHandle.getId();
    }

    @Override
    protected void proxyDisconnectFromRequestorToParent(final ObservableFuture<ConnectionHandle> disconnectFuture, final boolean causedByNetwork) {
        signalProvider.executor.execute(new Runnable() {
            @Override
            public void run() {
                Asserts.assertTrue(getDisconnectFuture() == disconnectFuture, "Bad futures 1");
                Asserts.assertTrue(disconnectFuture == signalProvider.disconnect(SignalProviderConnectionHandle.this, causedByNetwork), "Bad futures 2");
            }
        });
    }

    @Override
    public ObservableFuture<ConnectionHandle> reconnect() {
        final NestedObservableFuture<ConnectionHandle> future = new NestedObservableFuture<ConnectionHandle>(this);

        this.disconnect().addObserver(new Observer<ObservableFuture<ConnectionHandle>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectionHandle> item) {
                future.setNestedFuture(signalProvider.connect());
            }
        });

        LOGGER.error("You are very lazy and very very bad. You should feel shameful that this is not thread safe.");

        return future;
    }

    public ConnectionHandle getConnectionHandle() {
        return connectionHandle;
    }

    public void setConnectionHandle(ConnectionHandle connectionHandle) {
        this.connectionHandle = connectionHandle;
    }

    public ObservableFuture<Boolean> write(Object object) {
        if (connectionHandle instanceof Writable) {
            return ((Writable) connectionHandle).write(object);
        }

        throw new IllegalStateException("The connection is not writable " + connectionHandle);
    }

    /**
     * Is this connection the same connection that i'm wrapping?
     *
     * @param connectionHandle
     * @return
     */
    public boolean isFor(ConnectionHandle connectionHandle) {
       return this.connectionHandle == connectionHandle;
    }

    @Override
    public String toString() {
        return String.format("[SignalProviderConnectionHandle: %s]", connectionHandle);
    }

    @Override
    protected void onDestroy() {
        // we were destroyed!
    }
}
