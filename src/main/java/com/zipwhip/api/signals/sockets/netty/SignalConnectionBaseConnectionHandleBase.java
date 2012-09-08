package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.Writable;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionHandleBase;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.util.Asserts;

/**
 * Created with IntelliJ IDEA.
 * User: msmyers
 * Date: 9/5/12
 * Time: 3:37 PM
 *
 * This is for "connections" that sit inside SignalConnectionBase. It follows the naming pattern of [class][base] even
 * though it's redundant. I'm still looking for a better name than "Connection" since we have so many connection
 * objects floating around.
 */
public abstract class SignalConnectionBaseConnectionHandleBase extends ConnectionHandleBase implements Writable {

    private SignalConnectionBase signalConnection;

    public SignalConnectionBaseConnectionHandleBase(long id, SignalConnectionBase signalConnection) {
        super(id);

        this.signalConnection = signalConnection;
    }

    @Override
    protected void proxyDisconnectFromRequestorToParent(final ObservableFuture<ConnectionHandle> disconnectFuture, final boolean causedByNetwork) {
        signalConnection.executor.execute(new Runnable() {
            @Override
            public void run() {

                synchronized (SignalConnectionBaseConnectionHandleBase.this) {
                    if (isDestroyed()) {
                        // how did this happen??
                        if (!disconnectFuture.isDone()) {
                            throw new IllegalStateException("The future is not done, but the connection is destroyed??");
                        }
                    }

                    // the disconnectFuture that it returns will be the exact same future we already have
                    Asserts.assertTrue(
                            getDisconnectFuture() == disconnectFuture &&
                                    disconnectFuture == signalConnection.disconnect(SignalConnectionBaseConnectionHandleBase.this, causedByNetwork), "The disconnectFutures must match!");

                }
            }
        });
    }

    @Override
    public synchronized ObservableFuture<ConnectionHandle> reconnect() {
        return signalConnection.reconnect(this);
    }

    @Override
    public ObservableFuture<Boolean> write(Object object) {
        return signalConnection.send((SerializingCommand) object);
    }

    @Override
    protected void onDestroy() {

    }
}
