package com.zipwhip.api.signals.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zipwhip.util.StringUtil;

import java.util.Set;
import java.util.TreeSet;

/**
 * Date: 8/22/13
 * Time: 5:08 PM
 *
 * @author Michael
 * @version 1
 */
public class GsonUtil {

    public static String getString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        return element.getAsString();
    }

    public static Set<String> getSet(JsonElement element) {
        Set<String> result = new TreeSet<String>();

        JsonArray array = (JsonArray) element;

        for (JsonElement jsonElement : array) {
            String value = getString(jsonElement);
            if (StringUtil.isNullOrEmpty(value)) {
                continue;
            }

            result.add(value);
        }

        return result;
    }
}
