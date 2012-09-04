package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.presence.Presence;

import java.util.Map;
import java.util.concurrent.Callable;

/**
* Created by IntelliJ IDEA.
* User: Russ
* Date: 8/30/12
* Time: 3:53 PM
*/
public class ConnectCommandTask implements Callable<ObservableFuture<ConnectCommand>> {

    private final String clientId;
    private final Presence presence;
    private final Map<String, Long> versions;
    private final SignalConnection connection;

    public ConnectCommandTask(SignalConnection connection, String clientId, Map<String, Long> versions, Presence presence) {
        this.connection = connection;
        this.clientId = clientId;
        this.presence = presence;
        this.versions = versions;
    }

    @Override
    public ObservableFuture<ConnectCommand> call() throws Exception {
        final ObservableFuture<Boolean> future;

        final ObservableFuture<ConnectCommand> result = new DefaultObservableFuture<ConnectCommand>(connection) {
            @Override
            public String toString() {
                return "ConnectCommandTaskFuture";
            }
        };

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

        result.addObserver(new Observer<ObservableFuture<ConnectCommand>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ConnectCommand> item) {
                connection.removeOnMessageReceivedObserver(onMessageReceivedObserver);
                connection.removeOnDisconnectObserver(onDisconnectObserver[0]);
            }
        });

        connection.onDisconnect(onDisconnectObserver[0]);
        connection.onMessageReceived(onMessageReceivedObserver);

        if (presence != null && clientId != null){
            presence.setAddress(new ClientAddress(clientId));
        }

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

        return result;
    }
}
