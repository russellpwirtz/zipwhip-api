package com.zipwhip.api.signals.sockets.netty.pipeline.handler;

import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 8/22/12
 * Time: 2:22 PM
 */
public class SocketIdleStateHandler extends IdleStateHandler {

    public SocketIdleStateHandler(Timer timer, int pingIntervalSeconds, int pongTimeoutSeconds) {

        /**
         * This constructor says that Netty should fire a IdleState.ALL_IDLE event every
         * {@code pingIntervalSeconds} if the connection has been quiet for that period.
         *
         * If should fire a IdleState.READER_IDLE event if the connection has been quiet
         * for {@code pingIntervalSeconds + pingIntervalSeconds}.
         */
        super(timer, pingIntervalSeconds + pongTimeoutSeconds, 0, pingIntervalSeconds);
    }

}
