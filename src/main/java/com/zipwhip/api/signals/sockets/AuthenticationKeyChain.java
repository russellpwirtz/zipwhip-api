package com.zipwhip.api.signals.sockets;

import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.ListDirectory;
import com.zipwhip.util.LocalDirectory;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/28/12
 * Time: 2:49 PM
 */
public class AuthenticationKeyChain extends DestroyableBase {

    private final LocalDirectory<String, String> authenticated = new ListDirectory<String, String>();
    private final LocalDirectory<String, String> attempting = new ListDirectory<String, String>();

    public void add(String clientId, String sessionKey) {
        attempting.add(clientId, sessionKey);
    }

    public void remove(String clientId, String sessionKey) {
        remove(authenticated, clientId,  sessionKey);
        remove(attempting, clientId,  sessionKey);
    }

    public void finish(String clientId, String sessionKey) {
        remove(attempting, clientId, sessionKey);
        authenticated.add(clientId, sessionKey);
    }

    private void remove(LocalDirectory<String, String> directory, String clientId, String sessionKey) {
        List<String> keys = (List<String>) directory.get(clientId);
        if (keys == null) {
            return;
        }

        keys.remove(sessionKey);
    }


    public boolean isAuthenticated(String clientId, String sessionKey) {
        List<String> keys = (List<String>) authenticated.get(clientId);
        if (keys == null) {
            return false;
        }

        return keys.contains(sessionKey);
    }

    @Override
    protected void onDestroy() {
        authenticated.clear();
    }
}
