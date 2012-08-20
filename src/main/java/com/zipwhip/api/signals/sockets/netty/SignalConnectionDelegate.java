package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.lifecycle.DestroyableBase;
import org.jboss.netty.util.Timer;

import java.util.Observable;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/14/12
 * Time: 3:36 PM
 *
 * We can't allow the connections to talk with the SignalConnection directly. The problem is that they operate on a number
 * of crazy threads and theoretically 2 ChannelHandlers will access the same state of the parent object.
 *
 * To solve this problem we need a way to destroy the ChannelHandler.. But we can't since it's in a list. The other
 * suggestion is to compare the channel on-create to the channel on-event. However we can't pass it into the constructor
 * so that defeats the purpose.
 *
 * So we're going to use a Delegate that we can destroy. That way we can say: "Hey you're stale, kill yourself"
 */
public class SignalConnectionDelegate extends DestroyableBase {

    final SignalConnectionBase connection;

    public SignalConnectionDelegate(SignalConnectionBase connection) {
        this.connection = connection;
    }

    public synchronized void receivePong(PingPongCommand msg) {
        ensureValid();

        connection.receivePong(msg);
    }

    /**
     * The decision was to throw rather than return false
     */
    private synchronized void ensureValid() {
        if (isDestroyed()) {
            throw new IllegalStateException("The delegate was torn down, though later used.");
        }
    }

    /**
     * The caller (a ChannelHandler) will want to talk to the connection. We need to only forward the requests if allowed.
     *
     * @param handler
     * @param command
     */
    public void notifyReceiveEvent(NettyChannelHandler handler, Command command) {
        ensureValid();

        connection.receiveEvent.notifyObservers(handler, command);
    }

    public void disconnect(Boolean network) {
        ensureValid();

        connection.disconnect(network);
    }

    public void schedulePing(boolean now) {
        // We have activity on the wire, reschedule the next PING
        if (connection.doKeepalives) {
            connection.schedulePing(false);
        }
    }

    public synchronized void startReconnectStrategy() {
        ensureValid();

        connection.reconnectStrategy.start();
    }

    public synchronized void notifyConnect(Object sender, Boolean result) {
        ensureValid();

        connection.connectEvent.notifyObservers(sender, result);
    }

    public synchronized void notifyException(Object sender, String result) {
        ensureValid();

        connection.exceptionEvent.notifyObservers(sender, result);
    }

    public void setNetworkDisconnect(boolean networkDisconnect) {
        ensureValid();

        connection.networkDisconnect = networkDisconnect;
    }

    public void stopReconnectStrategy() {
        ensureValid();

        connection.reconnectStrategy.stop();
    }

    public synchronized void cancelOutstandingPingPongs() {
        ensureValid();

        connection.cancelOutstandingPingPongs();
    }

//    public void setWrapper(ChannelWrapper wrapper) {
//        ensureValid();
//
//        connection.wrapper = wrapper;
//    }

    public synchronized void notifyDisconnect(Object sender, boolean networkDisconnect) {
        ensureValid();

        connection.disconnectEvent.notifyObservers(sender, networkDisconnect);
    }

    @Override
    protected synchronized void onDestroy() {

    }
}
