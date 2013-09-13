package com.zipwhip.api.signals.dto.json;

import com.google.gson.*;
import com.zipwhip.api.signals.dto.BindResult;
import com.zipwhip.gson.GsonUtil;

import java.lang.reflect.Type;

/**
 * Date: 8/27/13
 * Time: 4:31 PM
 *
 * @author Michael
 * @version 1
 */
public class BindResponseTypeAdapter implements JsonDeserializer<BindResult> {

    @Override
    public BindResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = (JsonObject)json;

        String clientId = GsonUtil.getString(object, "clientId");
        String token = GsonUtil.getString(object, "token");
        long timestamp = GsonUtil.getLong(object.get("token"));

        return new BindResult(clientId, token, timestamp);
    }
}
