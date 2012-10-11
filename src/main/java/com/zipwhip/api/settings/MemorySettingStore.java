package com.zipwhip.api.settings;

import com.zipwhip.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/12/11
 * Time: 11:16 AM
 *
 * A memory only setting store.
 *
 */
public class MemorySettingStore implements SettingsStore {

    private static Logger logger = LoggerFactory.getLogger(MemorySettingStore.class);

    private Map<String, String> memoryStore = new HashMap<String, String>();

    @Override
    public void put(Keys key, String value) {

        logger.debug("Putting " + key.toString() + " = " + value);

        memoryStore.put(key.toString(), value);
    }

    @Override
    public String get(Keys key) {

        String value = memoryStore.get(key.toString());

        if (StringUtil.isNullOrEmpty(value)) {
            value = StringUtil.EMPTY_STRING;
        }

        logger.debug("Got " + value + " for key " + key.toString());

        return value;
    }

    @Override
    public void remove(Keys key) {

        logger.debug("Removing " + key.toString());

        memoryStore.remove(key.toString());
    }

    @Override
    public void clear() {

        logger.debug("Clearing all keys");

        for (Keys settingKey : Keys.values()) {
            memoryStore.remove(settingKey.toString());
        }
    }


}
