package com.zipwhip.api.signals;

import com.zipwhip.lifecycle.Destroyable;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/31/11
 * Time: 10:43 AM
 */
public interface ReconnectStrategy extends Destroyable {

//    public static void main(String[] args) {
//
//        SignalConnection signalConnection = new NettySignalConnection();
//
//        ReconnectStrategy strategy = null;
//
//        strategy.setSignalConnection(signalConnection);
//
//        signalConnection.connect();
//        signalConnection.link(strategy);
//
//        signalConnection.setReconnectStrategy(new FallbackReconnectStrategy());
//
//    }


    /**
     * You enable it by setting this to non-null
     *
     * If your connection is "connected" it does nothing. If your connection is "alive" but not "connected" it will particpate.
     * It observes your "signalConnection" events to determine when state changes.
     *
     *
     * @param signalConnection
     */
    void setSignalConnection(SignalConnection signalConnection);

    /**
     * Tells you which connection it's managing
     *
     * @return
     */
    SignalConnection getSignalConnection();


}
