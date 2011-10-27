package com.zipwhip.api;

import com.zipwhip.api.settings.PreferencesSettingsStore;
import com.zipwhip.api.settings.SettingsStore;
import com.zipwhip.api.settings.SettingsVersionStore;
import com.zipwhip.api.settings.VersionStore;
import com.zipwhip.api.signals.SignalProvider;
import org.apache.log4j.Logger;

/**
  * A base class for future implementation to extend.
  * <p/>
  * It takes all the non-API specific stuff out of ZipwhipClient implementations.
  * <p/>
  * If some class wants to communicate with Zipwhip, then it needs to extend this
  * class. This class gives functionality that can be used to parse Zipwhip API.
  * This naming convention was copied from Spring (JmsSupport) base class.
*/
public abstract class ClientZipwhipNetworkSupport extends ZipwhipNetworkSupport {

    protected static final Logger LOGGER = Logger.getLogger(ClientZipwhipNetworkSupport.class);

    protected SignalProvider signalProvider;
    protected SettingsStore settingsStore = new PreferencesSettingsStore();
    protected VersionStore versionsStore = new SettingsVersionStore(settingsStore);

    public ClientZipwhipNetworkSupport(ApiConnection connection, SignalProvider signalProvider) {

        super(connection);

        if (signalProvider != null) {
            setSignalProvider(signalProvider);
            link(signalProvider);
        }
    }


    public SignalProvider getSignalProvider() {
        return signalProvider;
    }

    public void setSignalProvider(SignalProvider signalProvider) {
        this.signalProvider = signalProvider;
    }

    public SettingsStore getSettingsStore() {
        return settingsStore;
    }

    public void setSettingsStore(SettingsStore store) {
        this.settingsStore = store;
        this.versionsStore = new SettingsVersionStore(store);
    }

}
