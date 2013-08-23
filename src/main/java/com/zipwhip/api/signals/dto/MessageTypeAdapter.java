package com.zipwhip.api.signals.dto;

import com.google.gson.*;
import com.zipwhip.api.signals.SubscribeCompleteContent;
import com.zipwhip.signals.address.Address;
import com.zipwhip.signals.message.Message;
import com.zipwhip.util.StringUtil;

import java.lang.reflect.Type;

/**
 * Date: 8/22/13
 * Time: 4:43 PM
 *
 * @author Michael
 * @version 1
 */
public class MessageTypeAdapter implements JsonDeserializer<Message>, JsonSerializer<Message> {

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = (JsonObject)json;

        Message message = new Message();

        message.setAddress((Address) context.deserialize(object.getAsJsonObject("address"), Address.class));
        message.setTimestamp(getLong(object.get("timestamp")));
        message.setId(object.get("id").getAsString());
        message.setEvent(object.get("event").getAsString());
        message.setType(object.get("type").getAsString());

        if (StringUtil.equalsIgnoreCase(message.getType(), "subscribe")) {
            if (StringUtil.equalsIgnoreCase(message.getEvent(), "complete")) {
                message.setContent(context.<SubscribeCompleteContent>deserialize(object.get("content"), SubscribeCompleteContent.class));
            }
        }

        return message;
    }

    private static long getLong(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return 0;
        }

        return element.getAsLong();
    }

    @Override
    public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
        return null;
    }
}
