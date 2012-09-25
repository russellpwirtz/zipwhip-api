package com.zipwhip.api.signals.sockets;

import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.lifecycle.CascadingDestroyableBase;

/**
 * Created with IntelliJ IDEA.
 * User: msmyers
 * Date: 9/4/12
 * Time: 4:51 PM
 *
 * Helpful base class for the "connection" object.
 */
public abstract class ConnectionHandleBase extends CascadingDestroyableBase implements ConnectionHandle {

    private ObservableFuture<ConnectionHandle> disconnectFuture;
    private boolean disconnectRequested = false;
    public boolean causedByNetwork = false;
    private final long id;

    protected ConnectionHandleBase(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public boolean disconnectedViaNetwork() {
        return causedByNetwork;
    }

    @Override
    public ObservableFuture<ConnectionHandle> disconnect() {
        return disconnect(false);
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> disconnect(boolean causedByNetwork) {
        final ObservableFuture<ConnectionHandle> disconnectFuture = getDisconnectFuture();
        if (isDestroyed() || disconnectFuture.isDone()) {
            return disconnectFuture;
        }

        if (this.disconnectRequested) {
            return disconnectFuture;
        }
        this.disconnectRequested = true;
        this.causedByNetwork = causedByNetwork;

        proxyDisconnectFromRequestorToParent(disconnectFuture, causedByNetwork);

        return disconnectFuture;
    }

    @Override
    public ObservableFuture<ConnectionHandle> getDisconnectFuture() {
        if (disconnectFuture ==  null) {
            synchronized (this) {
                if (disconnectFuture == null) {
                    disconnectFuture = new DefaultObservableFuture<ConnectionHandle>(this);
                }
            }
        }

        return disconnectFuture;
    }

    protected abstract void proxyDisconnectFromRequestorToParent(ObservableFuture<ConnectionHandle> disconnectFuture, boolean causedByNetwork);

}
