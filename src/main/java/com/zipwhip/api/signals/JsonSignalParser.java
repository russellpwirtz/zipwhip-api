package com.zipwhip.api.signals;

import com.zipwhip.api.response.JsonDtoParser;
import com.zipwhip.locators.Locator;
import com.zipwhip.util.MemoryLocator;
import com.zipwhip.util.Parser;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 6/27/11 Time: 5:21 PM
 */
public class JsonSignalParser implements SignalParser<JSONObject> {

    private static final Logger LOGGER = Logger.getLogger(JsonSignalParser.class);

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

        if (!object.has("signal")) {
            return null;
        }

        JSONObject node = object.optJSONObject("signal");
        if (node == null) {
            return null;
        }

        // Get signal.content
        JSONObject content = node.optJSONObject("content");

        if (LOGGER.isDebugEnabled()){
            LOGGER.debug("SIGNAL>>>" + node.toString());
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

}
