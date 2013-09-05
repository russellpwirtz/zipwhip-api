package com.zipwhip.api.signals.dto.json;

import com.google.gson.*;
import com.zipwhip.api.signals.dto.BindResult;

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

        String clientId = object.getAsJsonPrimitive("clientId").getAsString();
        String token = object.getAsJsonPrimitive("token").getAsString();

        return new BindResult(clientId, token);
    }
}
