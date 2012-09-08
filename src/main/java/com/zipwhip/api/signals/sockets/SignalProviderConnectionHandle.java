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

    private final SocketSignalProvider signalProvider;
    public boolean finishedActionConnect = false;
    protected ConnectionHandle connectionHandle;

    public SignalProviderConnectionHandle(long id, SocketSignalProvider signalProvider) {
        super(id);
        this.signalProvider = signalProvider;
        this.connectionHandle = connectionHandle;
    }

    @Override
    public long getId() {
        return connectionHandle.getId();
    }

    @Override
    protected void proxyDisconnectFromRequestorToParent(ObservableFuture<ConnectionHandle> disconnectFuture, boolean causedByNetwork) {
        Asserts.assertTrue(getDisconnectFuture() == disconnectFuture, "Bad futures 1");
        Asserts.assertTrue(disconnectFuture == signalProvider.disconnect(causedByNetwork), "Bad futures 2");
    }

    @Override
    public ObservableFuture<ConnectionHandle> reconnect() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
    protected void onDestroy() {
        // we were destroyed!
    }
}
