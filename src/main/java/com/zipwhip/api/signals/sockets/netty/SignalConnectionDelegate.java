package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.lifecycle.DestroyableBase;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/14/12
 * Time: 3:36 PM
 * <p/>
 * We can't allow the ChannelHandlers to talk with the SignalConnection directly. The problem is that they operate on
 * a number of crazy threads and theoretically 2 ChannelHandlers will access the same state of the parent object.
 * <p/>
 * To solve this problem we need a way to destroy the ChannelHandler.. But we can't since it's in a list (and doesn't
 * extend Destroyable). The other suggestion is to compare the channel on-create to the channel on-event.
 * However we can't pass it into the constructor so that defeats the purpose.
 * <p/>
 * So we're going to use a Delegate that we can destroy. That way we can say: "Hey you're stale, block access"
 *
 * This was built for Netty but can be used for all underlying socket technology stacks.
 */
public class SignalConnectionDelegate extends DestroyableBase {

    private static final Logger LOGGER = Logger.getLogger(SignalConnectionDelegate.class);

    protected final SignalConnectionBase signalConnectionBase;
    protected ConnectionHandle connectionHandle;
    private boolean paused = false;

    public SignalConnectionDelegate(SignalConnectionBase signalConnectionBase) {
        this.signalConnectionBase = signalConnectionBase;
    }

    /**
     * Synchronizing this method causes a deadlock.
     *
     * @param network
     */
    public void disconnectAsyncIfActive(final Boolean network) {
        runIfActive(new Runnable() {
            @Override
            public void run() {
                // this connection wont let stale requests go through
                connectionHandle.disconnect(network);
            }
        });
    }

    public int getConnectTimeoutSeconds() {
        return signalConnectionBase.getConnectTimeoutSeconds();
    }

    public void sendAsyncIfActive(final SerializingCommand command) {
        runIfActive(new Runnable() {
            @Override
            public void run() {
                signalConnectionBase.send(connectionHandle, command);
            }
        });
    }

    public void receivePong(final PingPongCommand command) {
        runIfActive(new Runnable() {
            @Override
            public void run() {
                signalConnectionBase.receivePong(connectionHandle, command);
            }
        });
    }

    public void notifyReceiveEvent(final Command command) {
        runIfActive(new Runnable() {
            @Override
            public void run() {
                // this is already in the "executor" of the signalConnection.
                signalConnectionBase.notifyEvent(connectionHandle, signalConnectionBase.receiveEvent, command);
            }
        });
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
        runIfActive(new Runnable() {
            @Override
            public void run() {
                signalConnectionBase.notifyEvent(connectionHandle, signalConnectionBase.pingEvent, event);
            }
        });
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

            signalConnectionBase.runIfActive(connectionHandle, runnable);
        }
    }

    public ConnectionHandle getConnectionHandle() {
        return connectionHandle;
    }

    public void setConnectionHandle(ConnectionHandle connectionHandle) {
        this.connectionHandle = connectionHandle;
    }

    @Override
    protected synchronized void onDestroy() {
        connectionHandle = null;
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
