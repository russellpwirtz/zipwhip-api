package com.zipwhip.api.signals.important.connect;

import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.important.Worker;
import com.zipwhip.signals.presence.Presence;
import org.apache.log4j.Logger;

import java.lang.Boolean;import java.lang.Exception;import java.lang.Long;import java.lang.Object;import java.lang.Override;import java.lang.RuntimeException;import java.lang.String;import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 11:18 AM
 */
public class ConnectCommandWorker implements Worker<ConnectCommandParameters, ConnectCommand> {

    private static final Logger LOGGER = Logger.getLogger(ConnectCommandWorker.class);

    public static final String REQUEST_TYPE = "connect";

    private SignalConnection connection;

    public ConnectCommandWorker() {

    }

    public ConnectCommandWorker(SignalConnection connection) {
        this();
        this.connection = connection;
        if (this.connection == null){
            throw new IllegalArgumentException("The connection cannot be null");
        }
    }

    @Override
    public ObservableFuture<ConnectCommand> execute(final ConnectCommandParameters parameters) throws Exception {

        String clientId = parameters.getClientId();
        String sessionKey = parameters.getSessionKey();
        Map<String, Long> versions = parameters.getVersions();
        Presence presence = parameters.getPresence();

        final ObservableFuture<java.lang.Boolean> future;

        final ObservableFuture<ConnectCommand> result = new DefaultObservableFuture<ConnectCommand>(connection);
        final Observer<Boolean>[] onDisconnectObserver = new Observer[1];

        final Observer<Command> onMessageReceivedObserver = new Observer<Command>() {
            @Override
            public void notify(Object sender, Command item) {
                if (item instanceof ConnectCommand) {
                    connection.removeOnMessageReceivedObserver(this);
                    connection.removeOnDisconnectObserver(onDisconnectObserver[0]);

                    result.setSuccess((ConnectCommand) item);
                }
            }
        };

        onDisconnectObserver[0] = new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean causedByNetwork) {
                connection.removeOnDisconnectObserver(this);
                connection.removeOnMessageReceivedObserver(onMessageReceivedObserver);

                result.setFailure(new RuntimeException("Disconnected: " + causedByNetwork));
            }
        };

        connection.onDisconnect(onDisconnectObserver[0]);
        connection.onMessageReceived(onMessageReceivedObserver);

        future = connection.send(new ConnectCommand(clientId, versions, presence));

        future.addObserver(new Observer<ObservableFuture<Boolean>>() {
            @Override
            public void notify(Object sender, ObservableFuture<Boolean> sendRequestFuture) {
                // this means that the transmission to the server was successful.
                // it does NOT mean that the response has come back yet.
                if (sendRequestFuture.getResult() == Boolean.FALSE) {
                    connection.removeOnMessageReceivedObserver(onMessageReceivedObserver);
                    connection.removeOnDisconnectObserver(onDisconnectObserver[0]);

                    result.setFailure(sendRequestFuture.getCause());
                }
            }
        });

        result.addObserver(new Observer<ObservableFuture<ConnectCommand>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectCommand> item) {
                connection.removeOnMessageReceivedObserver(onMessageReceivedObserver);
                connection.removeOnDisconnectObserver(onDisconnectObserver[0]);
            }
        });

        return result;
    }

    public SignalConnection getConnection() {
        return connection;
    }

    public void setConnection(SignalConnection connection) {
        this.connection = connection;
    }
}
