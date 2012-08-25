package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.lifecycle.DestroyableBase;

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

    private final SignalConnectionBase connection;

    public SignalConnectionDelegate(SignalConnectionBase connection) {
        this.connection = connection;
    }

    public synchronized void disconnect(Boolean network) {
        if (isDestroyed()){
            return;
        }

        ensureValid();
        if (connection.isConnected()) {
            connection.disconnect(network);
        }
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public int getConnectTimeoutSeconds() {
        assert connection != null;
        return connection.getConnectTimeoutSeconds();
    }

    public synchronized void send(SerializingCommand command) {
        ensureValid();
        connection.send(command);
    }

    public synchronized void receivePong(PingPongCommand command) {
        ensureValid();
        connection.receivePong(command);
    }

    public synchronized void notifyReceiveEvent(NettyChannelHandler handler, Command command) {
        ensureValid();
        connection.receiveEvent.notifyObservers(handler, command);
    }

//    public synchronized void notifyDisconnect(Object sender, boolean networkDisconnect) {
//        ensureValid();
//        connection.disconnectEvent.notifyObservers(sender, networkDisconnect);
//    }

    public synchronized void notifyException(Object sender, String result) {
        if (isDestroyed()) {
            // we don't care if it's destroyed. dont crash.
            return;
        }

        connection.exceptionEvent.notifyObservers(sender, result);
    }

    public synchronized void notifyPingEvent(Object sender, PingEvent event) {
        ensureValid();
        connection.pingEvent.notify(sender, event);
    }

    public synchronized void startReconnectStrategy() {
        ensureValid();
        connection.reconnectStrategy.start();
    }

    public synchronized void stopReconnectStrategy() {
        ensureValid();
        connection.reconnectStrategy.stop();
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

    }

}
