package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.Writable;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.util.Asserts;

/**
 * Created with IntelliJ IDEA.
 * User: msmyers
 * Date: 9/4/12
 * Time: 4:48 PM
 *
 *
 */
public class SignalProviderConnectionHandle extends ConnectionHandleBase implements Writable {

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
                Asserts.assertTrue(disconnectFuture == signalProvider.disconnect(causedByNetwork), "Bad futures 2");
            }
        });
    }

    @Override
    public ObservableFuture<ConnectionHandle> reconnect() {
        throw new RuntimeException("Not implemented");
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
        return String.format("SignalProviderConnectionHandle[%s]", connectionHandle);
    }

    @Override
    protected void onDestroy() {
        // we were destroyed!
    }
}
