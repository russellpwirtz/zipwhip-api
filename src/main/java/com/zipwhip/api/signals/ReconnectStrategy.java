package com.zipwhip.api.signals;

import com.zipwhip.lifecycle.Destroyable;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/31/11
 * Time: 10:43 AM
 */
public interface ReconnectStrategy extends Destroyable {

    /**
     *
     * @param connection
     */
    void requestReconnect(SignalConnection connection);

    /**
     *
     * @param connection
     * @param requestedDelay
     */
    void requestReconnect(SignalConnection connection, long requestedDelay);

}
