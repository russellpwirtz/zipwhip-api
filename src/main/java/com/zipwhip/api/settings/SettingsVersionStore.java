package com.zipwhip.api.settings;

import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/6/11
 * Time: 6:53 PM
 */
public class SettingsVersionStore implements VersionStore {

    private SettingsStore settingsStore;

    private Map<String, Long> memoryVersions = new HashMap<String, Long>();

    private static Logger logger = Logger.getLogger(SettingsVersionStore.class);

    /**
     * Create a new SettingsVersionStore using the settingsStore as the underlying storage solution.
     *
     * @param settingsStore The settingsStore to use as the underlying storage solution.
     */
    public SettingsVersionStore(SettingsStore settingsStore) {

        this.settingsStore = settingsStore;

        // Prefetch any persisted versions
        loadVersions();
    }

    @Override
    public Map<String, Long> get() {

        logger.debug("Getting versions " + memoryVersions.toString());

        return memoryVersions;
    }

    @Override
    public Long get(String versionKey) throws Exception {

        logger.debug("Getting version for key " + versionKey);

        return memoryVersions.get(versionKey);
    }

    @Override
    public boolean set(String versionKey, long newVersion) {

        logger.debug("Setting version " + versionKey + " : " + newVersion);

        Long previousVersion = memoryVersions.put(versionKey, newVersion);
        settingsStore.put(SettingsStore.Keys.VERSIONS, new JSONObject(memoryVersions).toString());

        return previousVersion == null || previousVersion < newVersion;
    }

    @Override
    public void clear() {

        logger.debug("Clearing versions from disk and memory");

        settingsStore.remove(SettingsStore.Keys.VERSIONS);

        memoryVersions.clear();
    }

    private void loadVersions() {

        memoryVersions.clear();

        String versionsString = settingsStore.get(SettingsStore.Keys.VERSIONS);

        if (StringUtil.exists(versionsString)) {
            try {
                JSONObject jsonVersions = new JSONObject(versionsString);
                Iterator iterator = jsonVersions.keys();

                while (iterator.hasNext()) {

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
