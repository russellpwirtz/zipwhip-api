package com.zipwhip.api.signals.dto;

import com.google.gson.*;
import com.zipwhip.signals.message.Message;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.TreeSet;

/**
 * Date: 8/22/13
 * Time: 4:56 PM
 *
 * @author Michael
 * @version 1
 */
public class DeliveredMessageTypeAdapter implements JsonSerializer<DeliveredMessage>, JsonDeserializer<DeliveredMessage> {
    @Override
    public DeliveredMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = (JsonObject)json;

        DeliveredMessage message = new DeliveredMessage();

        message.setMessage(context.<Message>deserialize(object.get("message"), Message.class));

        JsonArray array = object.getAsJsonArray("subscriptionIds");
        if (array != null && !array.isJsonNull() && array.size() > 0) {
            Set<String> strings = new TreeSet<String>();
            for (JsonElement e : array) {
                if (e == null || e.isJsonNull()) {
                    continue;
                }

                strings.add(e.getAsString());
            }

            message.setSubscriptionIds(strings);
        }

        return message;
    }

    @Override
    public JsonElement serialize(DeliveredMessage src, Type typeOfSrc, JsonSerializationContext context) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
