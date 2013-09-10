package com.zipwhip.api.signals.dto;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.zipwhip.util.DataConversionException;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: 9/10/13
 * Time: 2:41 PM
 *
 * I don't like putting this logic centralized in a static utility class. However, for sake of expediency
 * of development, it's convenient to do this instead of muck around with adding zipwhip-common-util as a dependency
 * for signals-api.
 *
 * @author Michael
 * @version 1
 */
public class SignalContentConverter {

    private Gson gson;

    public <T> T fromMap(Class<T> clazz, Map<String, Object> map) throws DataConversionException {

    }

    public <T> T fromJson(Class<T> clazz, JsonElement map) throws DataConversionException {

    }

    public HashMap<String, Object> toMap(SignalContact content) throws DataConversionException {
        return null;
    }

    public HashMap<String, Object> toMap(SignalMessage content) throws DataConversionException {
        return null;
    }

    public Gson getGson() {
        return gson;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }
}
