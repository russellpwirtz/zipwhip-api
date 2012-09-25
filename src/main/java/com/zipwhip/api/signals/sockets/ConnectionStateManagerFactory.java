package com.zipwhip.api.signals.sockets;

import com.zipwhip.util.Directory;
import com.zipwhip.util.Factory;
import com.zipwhip.util.SetDirectory;
import com.zipwhip.util.StateManager;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/28/12
 * Time: 3:00 PM
 */
public class ConnectionStateManagerFactory implements Factory<StateManager<ConnectionState>> {

    private static final Directory<ConnectionState, ConnectionState> directory = new SetDirectory<ConnectionState, ConnectionState>();

    static {
        directory.add(ConnectionState.NONE, ConnectionState.CONNECTING);
        directory.add(ConnectionState.CONNECTING, ConnectionState.CONNECTED);
        directory.add(ConnectionState.CONNECTING, ConnectionState.DISCONNECTED);
        directory.add(ConnectionState.CONNECTING, ConnectionState.DISCONNECTING);

        directory.add(ConnectionState.CONNECTED, ConnectionState.AUTHENTICATED);
        directory.add(ConnectionState.CONNECTED, ConnectionState.DISCONNECTING);
        directory.add(ConnectionState.CONNECTED, ConnectionState.DISCONNECTED);

        directory.add(ConnectionState.AUTHENTICATED, ConnectionState.CONNECTED);
        directory.add(ConnectionState.AUTHENTICATED, ConnectionState.DISCONNECTING);
        directory.add(ConnectionState.AUTHENTICATED, ConnectionState.DISCONNECTED);

        directory.add(ConnectionState.DISCONNECTING, ConnectionState.DISCONNECTED);

        directory.add(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING);

        // this one is for the reconnect due to ReconnectStrategy (ie: not started by us)
        directory.add(ConnectionState.DISCONNECTED, ConnectionState.CONNECTED);
    }

    private static final Factory<StateManager<ConnectionState>> INSTANCE = new ConnectionStateManagerFactory();

    @Override
    public StateManager<ConnectionState> create() {
        StateManager<ConnectionState> result = new StateManager<ConnectionState>(directory);

        result.set(ConnectionState.NONE);

        return result;
    }

    public static StateManager<ConnectionState> newStateManager() {
        try {
            return getInstance().create();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Factory<StateManager<ConnectionState>> getInstance() {
        return INSTANCE;
    }
}
