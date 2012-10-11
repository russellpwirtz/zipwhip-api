package com.zipwhip.api.signals;

import com.zipwhip.api.response.JsonDtoParser;
import com.zipwhip.locators.Locator;
import com.zipwhip.util.MemoryLocator;
import com.zipwhip.util.Parser;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 6/27/11 Time: 5:21 PM
 */
public class JsonSignalParser implements SignalParser<JSONObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonSignalParser.class);

    private final Locator<Parser<JSONObject, ?>> LOCATOR;

    public static final String CONTACT_KEY = "contact";
    public static final String CONVERSATION_KEY = "conversation";
    public static final String DEVICE_KEY = "device";
    public static final String MESSAGE_KEY = "message";
    public static final String CARBON_KEY = "carbon";

    public JsonSignalParser() {

        JsonDtoParser dtoParser = new JsonDtoParser();

        Map<String, Parser<JSONObject, ?>> elements = new HashMap<String, Parser<JSONObject, ?>>(5);

        elements.put(CONTACT_KEY, dtoParser.CONTACT_PARSER);
        elements.put(CONVERSATION_KEY, dtoParser.CONVERSATION_PARSER);
        elements.put(DEVICE_KEY, dtoParser.DEVICE_PARSER);
        elements.put(MESSAGE_KEY, dtoParser.MESSAGE_PARSER);
        elements.put(CARBON_KEY, dtoParser.CARBON_PARSER);

        LOCATOR = new MemoryLocator<Parser<JSONObject, ?>>(elements);
    }

    @Override
    public Signal parseSignal(JSONObject object) throws Exception {

        JSONObject node;

        if (object.has("signal")) {
            node = object.optJSONObject("signal");
        } else {
            node = object;
        }

        if (node == null) {
            return null;
        }

        // Get signal.content
        JSONObject content = node.optJSONObject("content");

        if (LOGGER.isDebugEnabled()){
            LOGGER.debug("SIGNAL>>>" + JsonSignalParser.hashMessageBody(node.toString()));
        }

        Signal signal = new JsonSignal(node.toString());

        String mType = signal.type = node.optString("type");
        signal.event = node.optString("event");
        signal.reason = node.optString("reason");
        signal.uuid = node.optString("uuid");
        signal.scope = node.optString("scope");
        signal.uri = node.optString("uri");

        Parser<JSONObject, ?> parser = LOCATOR.locate(mType.toLowerCase());

        if (parser != null){

            signal.content = parser.parse(content);

        } else {

            if (LOGGER.isDebugEnabled()){
                LOGGER.debug("Unparsed signal type: " + mType);
            }

            signal.content = node.optString("content");
        }

        return signal;
    }

    /**
     * FOR DEBUG ONLY! Use this to obfuscate the message body for use in logging.
     *
     * @param rawSignal The signal string from which the message body should be replaced with its hash code.
     * @return The signal string with its body should be replaced with its hash code or the original string if no body is found.
     */
    public static String hashMessageBody(String rawSignal) {

        int bodyStart = rawSignal.indexOf("\"body\":\"");

        if (bodyStart >= 0) {

            bodyStart += 8;
            int bodyEnd = rawSignal.indexOf("\"", bodyStart + 1);
            String body = rawSignal.substring(bodyStart, bodyEnd);
            return rawSignal.replace(body, String.valueOf(body.hashCode()));

        } else {

            bodyStart = rawSignal.indexOf("\"lastMessageBody\":\"");

            if (bodyStart >= 0) {
                bodyStart += 19;
                int bodyEnd = rawSignal.indexOf("\"", bodyStart + 1);
                String body = rawSignal.substring(bodyStart, bodyEnd);
                return rawSignal.replace(body, String.valueOf(body.hashCode()));
            } else {
                return rawSignal;
            }
        }
    }

}
