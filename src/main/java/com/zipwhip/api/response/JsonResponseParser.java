package com.zipwhip.api.response;

import com.zipwhip.api.dto.*;
import com.zipwhip.api.signals.PresenceUtil;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Date: Jul 18, 2009
 * Time: 10:22:28 AM
 * <p/>
 * Parses a ServerResponse from a Json string.
 */
public class JsonResponseParser implements ResponseParser {

    private static final Logger LOGGER = Logger.getLogger(JsonResponseParser.class);
    private static final String EMPTY_OBJECT = "{}";

    private JsonDtoParser parser = new JsonDtoParser();

    @Override
    public ServerResponse parse(String response) throws JSONException {

        LOGGER.debug("Parsing" + response);

        if (StringUtil.isNullOrEmpty(response)) {
            return null;
        } else if (StringUtil.equalsIgnoreCase("null", response)) {
            return null;
        }

        JSONObject thing = new JSONObject(response);
        String responseKey = "response";

        boolean success = thing.getBoolean("success");
        Map<String, Map<String, List<Signal>>> sessions = null;

        JSONObject sessionsObject = thing.optJSONObject("sessions");

        // IS THIS A COMPLEX OBJECT?
        JSONObject jsonObject = thing.optJSONObject(responseKey);
        if (jsonObject != null) {
            return new ObjectServerResponse(response, success, jsonObject, sessions);
        }

        // IS THIS AN ARRAY?
        JSONArray jsonArray = thing.optJSONArray(responseKey);
        if (jsonArray != null) {
            return new ArrayServerResponse(response, success, jsonArray, sessions);
        }

        // IS THIS A STRING?
        String string = thing.optString(responseKey, null);

        // Unfortunately the JSON libs in Android coerce bool into Strings
        if (string != null && !string.equalsIgnoreCase("true") && !string.equalsIgnoreCase("false")) {
            // a string
            return new StringServerResponse(response, success, string, sessions);
        }

        /// THIS MUST BE A BOOLEAN
        boolean bool = thing.getBoolean(responseKey);
        return new BooleanServerResponse(response, success, bool, sessions);
    }

    @Override
    public List<MessageToken> parseMessageTokens(ServerResponse serverResponse) throws Exception {

        List<MessageToken> result = null;

        if (serverResponse instanceof ObjectServerResponse) {
            ObjectServerResponse r = (ObjectServerResponse) serverResponse;
            JSONArray array = r.response.getJSONArray("tokens");

            result = parser.parseMessageTokens(array);
        } else if (serverResponse instanceof ArrayServerResponse) {
            ArrayServerResponse cplx = (ArrayServerResponse) serverResponse;

            result = parser.parseMessageTokens(cplx.response);
        }

        return result;
    }

    @Override
    public Message parseMessage(ServerResponse serverResponse) throws Exception {
        Message result = null;
        if (serverResponse instanceof ObjectServerResponse) {
            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;

            return parser.parseMessage(cplx.response);
        }
        return result;
    }

    @Override
    public List<Message> parseMessages(ServerResponse serverResponse) throws Exception {

        if (!(serverResponse instanceof ArrayServerResponse)) {
            throw new Exception("ServerResponse must be an ArrayServerResponse");
        }

        List<Message> messages = new ArrayList<Message>();

        ArrayServerResponse array = (ArrayServerResponse) serverResponse;

        for (int i = 0; i < array.response.length(); i++) {
            messages.add(parser.parseMessage(array.response.getJSONObject(i)));
        }

        return messages;
    }

    @Override
    public String parseString(ServerResponse serverResponse) throws Exception {
        String result;
        if (serverResponse instanceof StringServerResponse) {
            StringServerResponse stringServerResponse = (StringServerResponse) serverResponse;
            result = stringServerResponse.response;
        } else {
            throw new Exception("Unknown or unexpected server response");
        }
        return result;
    }

    @Override
    public Contact parseContact(ServerResponse serverResponse) throws Exception {

        if (serverResponse instanceof ObjectServerResponse) {

            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;
            return parser.parseContact(cplx.response);

        } else {
            throw new Exception("ServerResponse must by an ObjectServerResponse");
        }
    }

    @Override
    public Contact parseUser(ServerResponse serverResponse) throws Exception {

        if (serverResponse instanceof ObjectServerResponse) {

            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;

            if (cplx.response.has("user")) {
                return parser.parseContact(cplx.response.optJSONObject("user"));
            } else {
                return null;
            }
        } else {
            throw new Exception("ServerResponse must by an ObjectServerResponse");
        }
    }

    @Override
    public List<Contact> parseContacts(ServerResponse serverResponse) throws Exception {

        if (!(serverResponse instanceof ArrayServerResponse)) {
            throw new Exception("ServerResponse must be an ArrayServerResponse");
        }

        List<Contact> contacts = new ArrayList<Contact>();

        ArrayServerResponse array = (ArrayServerResponse) serverResponse;

        for (int i = 0; i < array.response.length(); i++) {
            contacts.add(parser.parseContact(array.response.getJSONObject(i)));
        }

        return contacts;
    }

    @Override
    public Conversation parseConversation(ServerResponse serverResponse) throws Exception {

        if (serverResponse instanceof ObjectServerResponse) {

            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;
            return parser.parseConversation(cplx.response);

        } else {
            throw new Exception("ServerResponse must by an ObjectServerResponse");
        }
    }

    @Override
    public List<Conversation> parseConversations(ServerResponse serverResponse) throws Exception {

        if (!(serverResponse instanceof ArrayServerResponse)) {
            throw new Exception("ServerResponse must be an ArrayServerResponse");
        }

        List<Conversation> conversations = new ArrayList<Conversation>();

        ArrayServerResponse array = (ArrayServerResponse) serverResponse;

        for (int i = 0; i < array.response.length(); i++) {
            conversations.add(parser.parseConversation(array.response.getJSONObject(i)));
        }

        return conversations;
    }

    @Override
    public DeviceToken parseDeviceToken(ServerResponse serverResponse) throws Exception {

        DeviceToken result = null;

        if (serverResponse instanceof ObjectServerResponse) {
            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;
            result = new DeviceToken();
            result.setDevice(new Device());

            LOGGER.debug(cplx.response.toString());

            if (cplx.response.has("device")) {
                result.getDevice().setAddress(cplx.response.getJSONObject("device").getString("address"));
                result.getDevice().setDeviceNumber(cplx.response.getJSONObject("device").getInt("deviceId"));
                result.getDevice().setId(cplx.response.getJSONObject("device").getLong("id"));
            } else {
                result.getDevice().setAddress(cplx.response.getString("address"));
                result.getDevice().setId(cplx.response.getLong("deviceId"));
                result.getDevice().setDeviceNumber(cplx.response.getInt("deviceNumber"));
            }

            result.setApiKey(cplx.response.getString("apiKey"));
            result.setSecret(cplx.response.getString("secret"));
            result.setSessionKey(cplx.response.getString("sessionKey"));
        }
        return result;
    }

    @Override
    public List<Presence> parsePresence(ServerResponse serverResponse) throws Exception {
        return PresenceUtil.getInstance().parse(new JSONArray(serverResponse.getRaw()));
    }

    @Override
    public EnrollmentResult parseEnrollmentResult(ServerResponse serverResponse) throws Exception {

        EnrollmentResult result = null;

        if (serverResponse instanceof ObjectServerResponse) {

            ObjectServerResponse objectServerResponse = (ObjectServerResponse) serverResponse;
            result = new EnrollmentResult();

            result.setCarbonEnabled(objectServerResponse.response.optBoolean("carbonEnabled"));
            result.setCarbonInstalled(objectServerResponse.response.optBoolean("carbonInstalled"));
            result.setDeviceNumber(objectServerResponse.response.optInt("deviceNumber"));
        }

        return result;
    }

}
