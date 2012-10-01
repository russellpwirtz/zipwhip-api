package com.zipwhip.api.signals.sockets;

import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.lifecycle.CascadingDestroyable;
import com.zipwhip.lifecycle.Destroyable;

/**
 * Created with IntelliJ IDEA.
 * User: msmyers
 * Date: 9/4/12
 * Time: 12:06 PM
 *
 * This is my attempt to allow multiple threads/handlers to process events asynchronously without touching the wrong
 * connection. If the underlying connection breaks, then this object is destroyed. A synonym for this class would
 * be "RemoteControl" as the purpose of it is to remotely control the connection -at-arms-length. So any thread
 * can abuse/call .disconnect() and it will only take effect if-only-if it is currently active.
 *
 * The takeaway of this is that you can safely operate on on a connection without worrying if something changed
 * from underneath you while you were working on it.
 *
 * You never really know if something is "connected" at any given time, but you DO know if it's the currently
 * active connection and that it hasn't been destroyed yet.
 *
 * If you want to process something while this connection is active (disconnection not processed yet) try this:
 *
 * synchronized(connection) {
 *     if (!connection.isDestroyed()) {
 *         return;
 *     }
 *
 *     // do your work here.
 * }
 *
 * We are not allowed to destroy the object while you have this lock. Beware that you will sieze up the inner
 * workings of the connection if you hold the lock too long. Never block waiting for a future to complete while
 * holding the lock as it most certainly will create a deadlock.
 *
 * NOTE: If you did not create this ConnectionHandle it would be disastrous for you to destroy it.
 *
 * @return
 */
public interface ConnectionHandle {

    boolean disconnectedViaNetwork();

    /**
     * Every ConnectionDelegate will have a unique Id. This is how you can tell them apart.
     *
     * @return
     */
    long getId();

    ObservableFuture<ConnectionHandle> disconnect();
    ObservableFuture<ConnectionHandle> disconnect(boolean causedByNetwork);

    /**
     * When you destroy this object, cascade the destruction it to the "destroyable" passed in.
     *
     * @param destroyable
     */
    void link(Destroyable destroyable);

    /**
     * Prevent the cascading destruction.
     *
     * @param destroyable
     */
    void unlink(Destroyable destroyable);

    boolean isDestroyed();

    /**
     * If you want to see if this object is disconnected, use .getDisconnectFuture().isDone();
     *
     * It will be correct if the internet went down or if you manually called .disconnect();
     *
     * @return
     */
    ObservableFuture<ConnectionHandle> getDisconnectFuture();

    /**
     * Kills this current connection and connects again. Returns the new connection.
     *
     * @return
     */
    ObservableFuture<ConnectionHandle> reconnect();

}
