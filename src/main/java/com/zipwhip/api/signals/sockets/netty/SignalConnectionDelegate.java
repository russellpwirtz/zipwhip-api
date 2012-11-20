package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/14/12
 * Time: 3:36 PM
 * <p/>
 * We can't allow the ChannelHandlers to talk with the SignalConnection directly. The problem is that they operate on
 * a number of crazy threads and theoretically 2 ChannelHandlers will ensureAbleTo the same state of the parent object.
 * <p/>
 * To solve this problem we need a way to destroy the ChannelHandler.. But we can't since it's in a list (and doesn't
 * extend Destroyable). The other suggestion is to compare the channel on-create to the channel on-event.
 * However we can't pass it into the constructor so that defeats the purpose.
 * <p/>
 * So we're going to use a Delegate that we can destroy. That way we can say: "Hey you're stale, block ensureAbleTo"
 *
 * This was built for Netty but can be used for all underlying socket technology stacks.
 */
public class SignalConnectionDelegate extends DestroyableBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignalConnectionDelegate.class);

    protected final SignalConnectionBase signalConnectionBase;
    protected final ConnectionHandle connectionHandle;
    private boolean paused = false;

    public SignalConnectionDelegate(SignalConnectionBase signalConnectionBase, ConnectionHandle connectionHandle) {
        this.signalConnectionBase = signalConnectionBase;
        this.connectionHandle = connectionHandle;
    }

    /**
     * Synchronizing this method causes a deadlock.
     *
     * @param network
     */
    public void disconnectAsyncIfActive(final boolean network) {
        runIfActive(new Runnable() {
            @Override
            public void run() {
                // this connection wont let stale requests go through
                connectionHandle.disconnect(network);
            }
        });
    }

    public synchronized void sendAsyncIfActive(final SerializingCommand command) {
        if (isDestroyed() || isPaused()) {
            return;
        }

        signalConnectionBase.send(connectionHandle, command);
    }

    public void receivePong(final PingPongCommand command) {
        synchronized (this) {
            if (isDestroyed() || isPaused()) {
                return;
            }

            signalConnectionBase.receivePong(connectionHandle, command);
        }
    }

    public void notifyReceiveEvent(final Command command) {
        notifyIfActive(signalConnectionBase.receiveEvent, command);
    }

    public void notifyExceptionAndDisconnect(final String result) {
        runIfActive(new Runnable() {
            @Override
            public void run() {
                signalConnectionBase.notifyEvent(connectionHandle, signalConnectionBase.exceptionEvent, result);
                connectionHandle.disconnect(Boolean.TRUE);
            }
        });
    }

    public synchronized void notifyPingEvent(final PingEvent event) {
        notifyIfActive(signalConnectionBase.pingEvent, event);
    }

    private void runIfActive(Runnable runnable) {
        if (isPaused()) {
            LOGGER.debug("Paused so quitting.");
            return;
        } else if (isDestroyed()) {
            LOGGER.debug(toString() + ": Returning silently for runIfActive. We were destroyed.");
            return;
        }

        synchronized (this) {
            if (isDestroyed() || isPaused()) {
                LOGGER.warn(toString() + ": runIfActive failure! We were destroyed or paused!");
                return;
            }

            Asserts.assertTrue(connectionHandle != null, "No way this can be null!");

            signalConnectionBase.runIfActive(connectionHandle, runnable);
        }
    }

    private <T> void notifyIfActive(ObservableHelper<T> observableHelper, T data) {
        if (isPaused()) {
            LOGGER.debug("Paused so quitting.");
            return;
        } else if (isDestroyed()) {
            LOGGER.debug(toString() + ": Returning silently for runIfActive. We were destroyed.");
            return;
        }

        synchronized (this) {
            if (isDestroyed() || isPaused()) {
                LOGGER.warn(toString() + ": runIfActive failure! We were destroyed or paused!");
                return;
            }

            signalConnectionBase.runIfActive(connectionHandle, observableHelper, data);
        }
    }

    public ConnectionHandle getConnectionHandle() {
        return connectionHandle;
    }

    @Override
    protected synchronized void onDestroy() {
        LOGGER.debug(String.format("Destroying %s / %s", this, Thread.currentThread().toString()));
    }

    public synchronized void pause() {
        paused = true;
    }

    public synchronized void resume() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }
}
