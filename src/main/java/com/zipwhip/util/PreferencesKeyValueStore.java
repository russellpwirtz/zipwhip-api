package com.zipwhip.util;

import com.zipwhip.api.settings.SettingsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/13/12
 * Time: 5:19 PM
 *
 * Implements the KeyValueStore with local preferences.
 */
public class PreferencesKeyValueStore<K> implements KeyValueStore<K, String> {

    private static Logger LOGGER = LoggerFactory.getLogger(PreferencesKeyValueStore.class);

    private final Preferences preferences;

    public PreferencesKeyValueStore(Preferences preferences) {
        if (preferences == null){
            preferences = Preferences.userRoot().node(this.getClass().getCanonicalName());
        }
        this.preferences = preferences;
    }

    public PreferencesKeyValueStore() {
        this(null);
    }

    @Override
    public void put(K key, String value) {
        String k = validateKey(key);
        value = StringUtil.defaultValue(value, StringUtil.EMPTY_STRING);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("put [%s:%s]", k, value));
        }

        preferences.put(k, value);
    }

    @Override
    public String get(K key) {
        String k = validateKey(key);
        String value = preferences.get(k, StringUtil.EMPTY_STRING);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("get [%s:%s]", k, value));
        }

        return value;
    }

    @Override
    public void remove(K key) {
        String k = validateKey(key);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("remove [%s]", k));
        }

        preferences.remove(k);
    }

    @Override
    public void clear() {
        LOGGER.trace("clear");

        try {
            preferences.clear();
        } catch (BackingStoreException e) {
            LOGGER.error("Failed to clear preferences", e);
            throw new RuntimeException(e);
        }
    }

    private String validateKey(K key) {
        if (key == null){
            throw new IllegalArgumentException("Key cannot be null");
        }
        String result = key.toString();
        if (result == null){
            throw new IllegalArgumentException("Key.toString() cannot be null");
        }
        return result;
    }
}
