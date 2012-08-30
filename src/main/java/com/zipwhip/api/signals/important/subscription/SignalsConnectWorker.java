package com.zipwhip.api.signals.important.subscription;

import com.zipwhip.api.Connection;
import com.zipwhip.api.ZipwhipNetworkSupport;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.important.Worker;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 4:31 PM
 */
public class SignalsConnectWorker implements Worker<SignalsConnectParameters, SubscriptionCompleteCommand> {

    private static final Logger LOGGER = Logger.getLogger(SignalsConnectWorker.class);

    public static final String REQUEST_TYPE = "/signals/connect";

    private final Connection connection;
    private final SignalProvider signalProvider;

    public SignalsConnectWorker(Connection connection, SignalProvider signalProvider) {
        this.connection = connection;
        this.signalProvider = signalProvider;
    }

    @Override
    public ObservableFuture<SubscriptionCompleteCommand> execute(SignalsConnectParameters request) throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();

        params.put("sessions", request.getSessionKey());
        params.put("clientId", request.getClientId());

        final ObservableFuture<SubscriptionCompleteCommand> resultFuture = new DefaultObservableFuture<SubscriptionCompleteCommand>(this);
        ObservableFuture<String> sendFuture;

        final Observer<Boolean>[] onDisconnectObserver = new Observer[1];

        final Observer<SubscriptionCompleteCommand> onSubscriptionCompleteObserver = new Observer<SubscriptionCompleteCommand>() {
            @Override
            public void notify(Object sender, SubscriptionCompleteCommand item) {
                signalProvider.removeOnSubscriptionCompleteObserver(this);
                signalProvider.removeOnConnectionChangedObserver(onDisconnectObserver[0]);

                LOGGER.debug("Successing");
                resultFuture.setSuccess(item);
            }
        };

        onDisconnectObserver[0] = new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                // on any kind of connection change, we need to just abort
                signalProvider.removeOnSubscriptionCompleteObserver(onSubscriptionCompleteObserver);
                signalProvider.removeOnConnectionChangedObserver(onDisconnectObserver[0]);

                LOGGER.debug("Failing (disconected)");
                resultFuture.setFailure(new RuntimeException("Disconnected! " + item));
            }
        };

        signalProvider.onConnectionChanged(onDisconnectObserver[0]);
        signalProvider.onSubscriptionComplete(onSubscriptionCompleteObserver);

        sendFuture = connection.send(ZipwhipNetworkSupport.SIGNALS_CONNECT, params);

        sendFuture.addObserver(new Observer<ObservableFuture<String>>() {
            @Override
            public void notify(Object sender, ObservableFuture<String> item) {
                if (!item.isSuccess()) {
                    signalProvider.removeOnConnectionChangedObserver(onDisconnectObserver[0]);
                    signalProvider.removeOnSubscriptionCompleteObserver(onSubscriptionCompleteObserver);

                    LOGGER.debug("Failing (send failed?)");
                    if (item.isCancelled()) {
                        resultFuture.cancel();
                    } else if (item.getCause() != null) {
                        resultFuture.setFailure(item.getCause());
                    }
                } else {
                    LOGGER.debug("/signals/connect executed successfully. You should get back a SubscriptionCompleteCommand any time now.");
                }
            }
        });

        resultFuture.addObserver(new Observer<ObservableFuture<SubscriptionCompleteCommand>>() {

            @Override
            public void notify(Object sender, ObservableFuture<SubscriptionCompleteCommand> item) {
                signalProvider.removeOnConnectionChangedObserver(onDisconnectObserver[0]);
                signalProvider.removeOnSubscriptionCompleteObserver(onSubscriptionCompleteObserver);
            }
        });

        return resultFuture;
    }

    public Connection getConnection() {
        return connection;
    }

    public SignalProvider getSignalProvider() {
        return signalProvider;
    }

}
