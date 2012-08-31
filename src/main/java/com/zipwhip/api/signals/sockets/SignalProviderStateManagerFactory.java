package com.zipwhip.api.signals.sockets;

import com.zipwhip.util.Directory;
import com.zipwhip.util.Factory;
import com.zipwhip.util.SetDirectory;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/28/12
 * Time: 3:00 PM
 */
public class SignalProviderStateManagerFactory implements Factory<StateManager<SignalProviderState>> {

    private static final Directory<SignalProviderState, SignalProviderState> directory = new SetDirectory<SignalProviderState, SignalProviderState>();

    static {
        directory.add(SignalProviderState.NONE, SignalProviderState.CONNECTING);
        directory.add(SignalProviderState.CONNECTING, SignalProviderState.CONNECTED);
        directory.add(SignalProviderState.CONNECTING, SignalProviderState.DISCONNECTED);
        directory.add(SignalProviderState.CONNECTING, SignalProviderState.DISCONNECTING);

        directory.add(SignalProviderState.CONNECTED, SignalProviderState.AUTHENTICATED);
        directory.add(SignalProviderState.CONNECTED, SignalProviderState.DISCONNECTING);
        directory.add(SignalProviderState.CONNECTED, SignalProviderState.DISCONNECTED);

        directory.add(SignalProviderState.AUTHENTICATED, SignalProviderState.CONNECTED);
        directory.add(SignalProviderState.AUTHENTICATED, SignalProviderState.DISCONNECTING);
        directory.add(SignalProviderState.AUTHENTICATED, SignalProviderState.DISCONNECTED);

        directory.add(SignalProviderState.DISCONNECTING, SignalProviderState.DISCONNECTED);

        directory.add(SignalProviderState.DISCONNECTED, SignalProviderState.CONNECTING);

        // this one is for the reconnect due to ReconnectStrategy (ie: not started by us)
        directory.add(SignalProviderState.DISCONNECTED, SignalProviderState.CONNECTED);
    }

    private static final Factory<StateManager<SignalProviderState>> INSTANCE = new SignalProviderStateManagerFactory();

    @Override
    public StateManager<SignalProviderState> create() {
        StateManager<SignalProviderState> result = new StateManager<SignalProviderState>(directory);

        result.set(SignalProviderState.NONE);

        return result;
    }

    public static Factory<StateManager<SignalProviderState>> getInstance() {
        return INSTANCE;
    }
}
