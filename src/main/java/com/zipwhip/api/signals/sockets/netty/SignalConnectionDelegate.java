package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.lifecycle.DestroyableBase;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/14/12
 * Time: 3:36 PM
 * <p/>
 * We can't allow the connections to talk with the SignalConnection directly. The problem is that they operate on a number
 * of crazy threads and theoretically 2 ChannelHandlers will access the same state of the parent object.
 * <p/>
 * To solve this problem we need a way to destroy the ChannelHandler.. But we can't since it's in a list. The other
 * suggestion is to compare the channel on-create to the channel on-event. However we can't pass it into the constructor
 * so that defeats the purpose.
 * <p/>
 * So we're going to use a Delegate that we can destroy. That way we can say: "Hey you're stale, kill yourself"
 */
public class SignalConnectionDelegate extends DestroyableBase {

    private static final Logger LOGGER = Logger.getLogger(SignalConnectionDelegate.class);

    protected final SignalConnectionBase connection;
    protected ChannelWrapper channelWrapper;
    private boolean paused = false;

    public SignalConnectionDelegate(SignalConnectionBase connection) {
        this.connection = connection;
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
                if (network) {
                    LOGGER.debug(toString() + ": Calling disconnect() blindly without checking isConnected()");
                    connection.disconnect(true);
                } else {
                    if (connection.isConnected()) {
                        LOGGER.debug(toString() + ": Calling disconnect() because connected");
                        connection.disconnect(false);
                    } else {
                        LOGGER.debug(toString() + ": We didn't call disconnect because we were not connected!");
                    }
                }
            }
        });
    }

    private void runIfActive(Runnable runnable) {
        if (paused) {
            LOGGER.debug("Paused so quitting.");
            return;
        }
        // return without crashing
        if (isDestroyed()) {
            LOGGER.debug(toString() + ": Returning silently for runIfActive. We were destroyed.");
            return;
        }

        synchronized (this) {
            if (isDestroyed()) {
                LOGGER.warn(toString() + ": runIfActive failure! We were destroyed!");
                return;
            }

            connection.runIfActive(channelWrapper, runnable);
        }
    }

    public int getConnectTimeoutSeconds() {
        return connection.getConnectTimeoutSeconds();
    }

    public void send(final SerializingCommand command) {
        // prevent destruction between these two lines via sync block
        runIfActive(new Runnable() {
            @Override
            public void run() {
                connection.send(command);
            }
        });
    }

    public void receivePong(final PingPongCommand command) {
        runIfActive(new Runnable() {
            @Override
            public void run() {
                connection.receivePong(command);
            }
        });
    }

    public void notifyReceiveEvent(final NettyChannelHandler handler, final Command command) {
        runIfActive(new Runnable() {
            @Override
            public void run() {
                connection.receiveEvent.notifyObservers(handler, command);
            }
        });
    }

    public void notifyExceptionAndDisconnect(final Object sender, final String result) {
        runIfActive(new Runnable() {
            @Override
            public void run() {
                connection.exceptionEvent.notifyObservers(sender, result);
                connection.disconnect(Boolean.TRUE);
            }
        });
    }

    public synchronized void notifyPingEvent(final Object sender, final PingEvent event) {
        runIfActive(new Runnable() {
            @Override
            public void run() {
                connection.pingEvent.notify(sender, event);
            }
        });
    }

    /**
     * Throw rather than return false
     */
    private synchronized void ensureValid() {
        if (isDestroyed()) {
            throw new IllegalStateException("The delegate was torn down, though later used.");
        }
    }

    @Override
    protected synchronized void onDestroy() {
        channelWrapper = null;
    }

    public ChannelWrapper getChannelWrapper() {
        return channelWrapper;
    }

    public void setChannelWrapper(ChannelWrapper channelWrapper) {
        this.channelWrapper = channelWrapper;
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
