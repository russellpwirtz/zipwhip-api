package com.zipwhip.api.signals;

import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/31/11
 * Time: 5:29 PM
 */
public class DefaultSignalClientSettingsStore implements SignalClientSettingsStore {

    private static final String VERSIONS_KEY = "VERSIONS";

    private static Logger logger = Logger.getLogger(DefaultSignalClientSettingsStore.class);

    private Map<String, Long> memoryVersions = new HashMap<String, Long>();

    private Preferences prefs = Preferences.userRoot().node(DefaultSignalClientSettingsStore.class.getCanonicalName());

    public DefaultSignalClientSettingsStore() {
        // Prefetch any persisted versions
        loadVersions();
    }

    @Override
    public void put(Keys key, String value) {

        logger.debug("Putting " + key.toString() + " = " + value);

        prefs.put(key.toString(), value);
    }

    @Override
    public String get(Keys key) {

        String value = prefs.get(key.toString(), StringUtil.EMPTY_STRING);

        logger.debug("Got " + value + " for key " + key.toString());

        return value;
    }

    @Override
    public void clear(Keys key) {

        logger.debug("Removing " + key.toString());

        prefs.remove(key.toString());
    }

    @Override
    public void clearAll() {

        logger.debug("Clearing all keys");

        for (Keys settingKey : Keys.values()) {
            prefs.remove(settingKey.toString());
        }
    }

    @Override
    public Map<String, Long> getVersions() {

        logger.debug("Getting versions " + memoryVersions.toString());

        return memoryVersions;
    }

    @Override
    public boolean setVersion(String versionKey, Long newVersion) {

        logger.debug("Setting version " + versionKey + " : " + newVersion);

        Long previousVersion = memoryVersions.put(versionKey, newVersion);

        if (previousVersion != null && previousVersion > newVersion) {

            memoryVersions.put(versionKey, previousVersion);

            return false;

        } else {

            logger.debug("Appending version to disk");

            prefs.put(VERSIONS_KEY, new JSONObject(memoryVersions).toString());

            return true;
        }
    }

    @Override
    public void clearVersions() {

        logger.debug("Clearing versions from disk and memory");

        prefs.remove(VERSIONS_KEY);

        memoryVersions.clear();
    }

    private void loadVersions() {

        memoryVersions.clear();

        String versionsString = prefs.get(VERSIONS_KEY, StringUtil.EMPTY_STRING);

        if (StringUtil.exists(versionsString)) {
            try {
                JSONObject jsonVersions = new JSONObject(versionsString);
                Iterator iterator = jsonVersions.keys();

                if (iterator.hasNext()) {

                    String key = (String) iterator.next();
                    String ValueString = jsonVersions.optString(key, StringUtil.EMPTY_STRING);

                    memoryVersions.put(key, Long.parseLong(ValueString));
                }
            } catch (JSONException e) {
                logger.error("Error parsing versions JSON", e);
            }
        }
    }

}
