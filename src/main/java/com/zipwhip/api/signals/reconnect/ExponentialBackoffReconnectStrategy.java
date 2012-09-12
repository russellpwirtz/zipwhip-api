package com.zipwhip.api.signals.reconnect;

import com.zipwhip.reliable.retry.ExponentialBackoffRetryStrategy;
import org.jboss.netty.util.Timer;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/11/12
 * Time: 4:36 PM
 *
 *
 */
public class ExponentialBackoffReconnectStrategy extends DefaultReconnectStrategy {

    public ExponentialBackoffReconnectStrategy() {
        this(null);
    }

    public ExponentialBackoffReconnectStrategy(Timer timer) {
        super(timer, new ExponentialBackoffRetryStrategy(1000, 2.0));
    }

}
