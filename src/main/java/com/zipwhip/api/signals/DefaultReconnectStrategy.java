package com.zipwhip.api.signals;

import com.zipwhip.events.Observer;
import org.apache.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/7/11
 * Time: 2:29 PM
 */
public class DefaultReconnectStrategy implements ReconnectStrategy {

    private SignalConnection signalConnection;
    private Observer<Boolean> disconnectObserver;

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOGGER = Logger.getLogger(DefaultReconnectStrategy.class);

    private static final long RECONNECT_DELAY = 5000;

    @Override
    public void setSignalConnection(SignalConnection signalConnection) {
        this.signalConnection = signalConnection;
    }

    @Override
    public SignalConnection getSignalConnection() {
        return signalConnection;
    }

    @Override
    public void start() {
        if (signalConnection != null && disconnectObserver == null) {

            disconnectObserver = new Observer<Boolean>() {

                @Override
                public void notify(Object sender, Boolean reconnect) {

                    if (reconnect) {

                        LOGGER.debug("Reconnect requested from " + sender.getClass());

                        scheduler.schedule(new Runnable() {
                            @Override
                            public void run() {
                                try {

                                    Future<Boolean> task = signalConnection.connect();
                                    Boolean success = task.get();

                                    if (success) {
                                        LOGGER.debug("Reconnected successfully");
                                    } else {
                                        signalConnection.disconnect(true);
                                    }
                                } catch (Exception e) {
                                    LOGGER.error("Error reconnecting", e);
                                }
                            }
                        }, RECONNECT_DELAY, TimeUnit.MILLISECONDS);
                    }
                }
            };

            signalConnection.onDisconnect(disconnectObserver);
        }
    }

    @Override
    public void stop() {
        if (signalConnection != null && disconnectObserver != null) {
            signalConnection.removeOnDisconnectObserver(disconnectObserver);
        }
    }

}
