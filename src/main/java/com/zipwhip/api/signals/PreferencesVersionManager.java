package com.zipwhip.api.signals;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/31/11
 * Time: 3:21 PM
 * <p/>
 * A default implementation of VersionManager that uses Java Preferences as the persistence solution.
 */
public class PreferencesVersionManager implements VersionManager {

    private static Logger logger = Logger.getLogger(PreferencesVersionManager.class);

    private Preferences versionPreferences = Preferences.userRoot().node(PreferencesVersionManager.class.getCanonicalName());
    private Map<String, Long> memoryVersions = new HashMap<String, Long>();

    public PreferencesVersionManager() {
        loadVersions();
    }

    @Override
    public Map<String, Long> getVersions() {

        logger.debug("Getting versions " + memoryVersions.toString());

        return memoryVersions;
    }

    @Override
    public synchronized boolean setVersion(String versionKey, Long newVersion) {

        logger.debug("Setting version " + versionKey + " : " + newVersion);

        Long previousVersion = memoryVersions.put(versionKey, newVersion);

        if (previousVersion != null && previousVersion > newVersion) {

            memoryVersions.put(versionKey, previousVersion);

            return false;

        } else {

            logger.debug("Appending version to disk");

            versionPreferences.putLong(versionKey, newVersion);

            return true;
        }
    }

    @Override
    public synchronized void clearVersions() {

        try {
            logger.debug("Clearing versions from disk and memory");

            versionPreferences.clear();

            memoryVersions.clear();

        } catch (BackingStoreException e) {
            logger.error("Error clearing versions", e);
        }
    }

    private void loadVersions() {
        try {
            String[] keys = versionPreferences.keys();

            for (String key : keys) {

                long value = versionPreferences.getLong(key, -1);

                logger.debug("Loading key " + key + " and version " + value);

                if (value >= 0) {
                    memoryVersions.put(key, value);
                }
            }
        } catch (BackingStoreException e) {
            logger.error("Error loading versions", e);
        }
    }

}
