package com.zipwhip.api.signals.dto;

import com.google.gson.*;
import com.zipwhip.api.signals.SubscribeCompleteContent;
import com.zipwhip.signals.address.Address;
import com.zipwhip.signals.message.DefaultMessage;
import com.zipwhip.signals.message.Message;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * Date: 8/22/13
 * Time: 4:43 PM
 *
 * @author Michael
 * @version 1
 */
public class MessageTypeAdapter implements JsonDeserializer<Message>, JsonSerializer<Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageTypeAdapter.class);

    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = (JsonObject)json;

        DefaultMessage message = new DefaultMessage();

        message.setAddress((Address) context.deserialize(object.getAsJsonObject("address"), Address.class));
        message.setTimestamp(getLong(object.get("timestamp")));
        message.setId(object.get("id").getAsString());
        message.setEvent(object.get("event").getAsString());
        message.setType(object.get("type").getAsString());

        JsonElement content = object.get("content");

        if (StringUtil.equalsIgnoreCase(message.getType(), "subscribe")) {
            if (StringUtil.equalsIgnoreCase(message.getEvent(), "complete")) {
                message.setContent(context.<SubscribeCompleteContent>deserialize(content, SubscribeCompleteContent.class));
            }
        } else if (StringUtil.equalsIgnoreCase(message.getType(), "presence")) {
            message.setContent(context.<Presence>deserialize(content, Presence.class));
        }

        if (message.getContent() == null && !GsonUtil.isNull(content)) {
            Object _content = GsonUtil.getDefaultValue(getClass().getClassLoader(), context, content);

            if (!(_content instanceof Serializable)) {
                throw new JsonParseException("The content returned for was not serializable! " + _content);
            } else {
                message.setContent((Serializable) _content);
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
