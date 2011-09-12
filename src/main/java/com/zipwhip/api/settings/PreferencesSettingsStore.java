package com.zipwhip.api.settings;

import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.util.prefs.Preferences;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/31/11
 * Time: 5:29 PM
 */
public class PreferencesSettingsStore implements SettingsStore {

    private static Logger logger = Logger.getLogger(PreferencesSettingsStore.class);

    // Java's underlying, platform independent disk storage.
    private Preferences preferences = Preferences.userRoot().node(PreferencesSettingsStore.class.getCanonicalName());

    @Override
    public void put(Keys key, String value) {

        logger.debug("Putting " + key.toString() + " = " + value);

        preferences.put(key.toString(), value);
    }

    @Override
    public String get(Keys key) {

        String value = preferences.get(key.toString(), StringUtil.EMPTY_STRING);

        logger.debug("Got " + value + " for key " + key.toString());

        return value;
    }

    @Override
    public void remove(Keys key) {

        logger.debug("Removing " + key.toString());

        preferences.remove(key.toString());
    }

    @Override
    public void clear() {

        logger.debug("Clearing all keys");

        for (Keys settingKey : Keys.values()) {
            preferences.remove(settingKey.toString());
        }
    }

}
