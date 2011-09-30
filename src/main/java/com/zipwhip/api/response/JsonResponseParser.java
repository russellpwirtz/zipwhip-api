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

import java.util.List;
import java.util.Map;

/**
 * Date: Jul 18, 2009
 * Time: 10:22:28 AM
 * <p/>
 * Parses a ServerResponse from a Json string.
 */
public class JsonResponseParser implements ResponseParser {

    private static Logger logger = Logger.getLogger(JsonResponseParser.class);

    private static ResponseParser instance;
    private static final String EMPTY_OBJECT = "{}";

    private JsonDtoParser parser = JsonDtoParser.getInstance();

    public synchronized static ResponseParser getInstance() {
        if (instance == null){
            instance = new JsonResponseParser();
        }
        return instance;
    }

    @Override
    public ServerResponse parse(String response) throws JSONException {

        logger.debug("Parsing" + response);

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

        if (sessionsObject != null && !EMPTY_OBJECT.equals(sessionsObject.toString())) {
            logger.debug(":NOTNULL: " + sessionsObject);
        } else {
             logger.debug(":NULL: " + sessionsObject);
        }

        /// IS THIS A COMPLEX OBJECT?
        JSONObject jsonObject = thing.optJSONObject(responseKey);
        if (jsonObject != null) {
            return new ObjectServerResponse(response, success, jsonObject, sessions);
        }

        /// IS THIS AN ARRAY?
        JSONArray jsonArray = thing.optJSONArray(responseKey);
        if (jsonArray != null) {
            return new ArrayServerResponse(response, success, jsonArray, sessions);
        }

        /// IS THIS A STRING?
        String string = thing.optString(responseKey, null);
        if (string != null) {
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
    public String parseString(ServerResponse serverResponse) throws Exception {
        String result = null;
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
        if (!(serverResponse instanceof ObjectServerResponse)) {
            return null;
        }

        ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;

        return parser.parseContact(cplx.response);
    }

    @Override
    public DeviceToken parseDeviceToken(ServerResponse serverResponse) throws Exception {

        DeviceToken result = null;

        if (serverResponse instanceof ObjectServerResponse) {
            ObjectServerResponse cplx = (ObjectServerResponse) serverResponse;
            result = new DeviceToken();
            result.setDevice(new Device());

            System.out.println(cplx.response.toString());

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

}
