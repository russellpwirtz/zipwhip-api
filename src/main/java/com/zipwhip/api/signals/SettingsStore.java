package com.zipwhip.api.signals;

import com.zipwhip.lib.KeyValueStore;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/1/11
 * Time: 9:58 AM
 *
 * This is a facade interface that should be extended to implement your platform's preferred storage solution.
 * A default implementation is provided in
 *
 */
public interface SettingsStore extends KeyValueStore<SettingsStore.Keys, String>, VersionManager {

    public enum Keys { SESSION_KEY, CLIENT_ID }

}
