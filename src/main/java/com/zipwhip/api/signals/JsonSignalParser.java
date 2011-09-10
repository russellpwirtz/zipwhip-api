package com.zipwhip.api.signals;

import com.zipwhip.api.response.JsonDtoParser;
import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 6/27/11 Time: 5:21 PM
 */
public class JsonSignalParser implements SignalParser<JSONObject> {

    private static final Logger LOGGER = Logger.getLogger(JsonSignalParser.class);

    public static final String CONTACT_KEY = "contact";
    public static final String CONVERSATION_KEY = "conversation";
    public static final String DEVICE_KEY = "device";
    public static final String MESSAGE_PROGRESS_KEY = "messageProgress";
    public static final String MESSAGE_KEY = "message";
    public static final String CARBON_KEY = "carbon";

    private JsonDtoParser parser = JsonDtoParser.getInstance();

    private static JsonSignalParser instance;

    public static JsonSignalParser getInstance() {
        if (instance == null) {
            instance = new JsonSignalParser();
        }

        return instance;
    }

    @Override
    public Signal parseSignal(JSONObject object) throws Exception {

        if (!object.has("signal")) {
            return null;
        }

        JSONObject node = object.optJSONObject("signal");
        if (node == null) {
            return null;
        }

        // Get signal.content
        JSONObject content = node.optJSONObject("content");

        LOGGER.debug("SIGNAL>>>" + node.toString());

        Signal signal = new JsonSignal(node.toString());

        String mType = signal.type = node.optString("type");
        signal.event = node.optString("event");
        signal.reason = node.optString("reason");
        signal.uuid = node.optString("uuid");
        signal.scope = node.optString("scope");
        signal.uri = node.optString("uri");

        if (mType.equalsIgnoreCase(CONTACT_KEY)) {
            signal.content = parser.parseContact(content);
        } else if (mType.equalsIgnoreCase(CONVERSATION_KEY)) {
            signal.content = parser.parseConversation(content);
        } else if (mType.equalsIgnoreCase(DEVICE_KEY)) {
            signal.content = parser.parseDevice(content);
        } else if (mType.equalsIgnoreCase(MESSAGE_KEY)) {
            signal.content = parser.parseMessage(content);
        } else if (mType.equalsIgnoreCase(MESSAGE_PROGRESS_KEY)) {
            signal.content = parser.parseMessageProgress(content);
        } else if (mType.equalsIgnoreCase(CARBON_KEY)) {
            signal.content = parser.parseCarbonMessageContent(content);
        } else {
            LOGGER.debug("Unparsed signal type: " + mType);
            signal.content = node.optString("content");
        }

        return signal;
    }

}
