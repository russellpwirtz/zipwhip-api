package com.zipwhip.api.response;

import com.zipwhip.api.dto.SignalToken;
import com.zipwhip.api.signals.JsonSignalParser;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.subscriptions.SubscriptionEntry;
import com.zipwhip.util.Parser;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/12/11
 * Time: 3:41 PM
 *
 * Parses from JSON to SignalToken
 */
public class JsonSignalTokenParser implements Parser<String, SignalToken> {

    private static final Logger LOGGER = Logger.getLogger(JsonSignalTokenParser.class);

    private JsonSignalParser signalParser = new JsonSignalParser();

    @Override
    public SignalToken parse(String json) throws Exception {

        if (StringUtil.isNullOrEmpty(json)){
            throw new NullPointerException("The json is empty");
        }

        JSONObject object = new JSONObject(json);

        SignalToken result = new SignalToken();

        parseSubscriptionEntry(result, object);
        parseSignals(result, object);
        parseExtras(result, object);

        return result;
    }

    private void parseSignals(SignalToken result, JSONObject object) throws JSONException {

        List<Signal> signals = new ArrayList<Signal>();

        JSONArray signalArray = object.getJSONArray("signals");

        for (int i = 0; i < signalArray.length(); i++) {

            Signal signal = null;

            try {
                signal = signalParser.parseSignal(signalArray.optJSONObject(i));
            } catch (Exception e) {
                LOGGER.error("Error parsing signal from signal token", e);
            }

            if (signal != null) {
                signals.add(signal);
            }
        }

        result.setSignals(signals);
    }

    private void parseExtras(SignalToken result, JSONObject object) {
        result.setDeviceAddress(object.optString("deviceAddress"));
        result.setMobileNumber(object.optString("mobileNumber"));
    }

    private void parseSubscriptionEntry(SignalToken result, JSONObject object) throws JSONException {

        SubscriptionEntry subscriptionEntry = new SubscriptionEntry();

        object = object.getJSONObject("subscriptionEntry");

        subscriptionEntry.setEncodedSubscriptionSettings(object.optString("encodedSubscriptionSettings"));
        subscriptionEntry.setSignalFilters(object.optString("signalFilters"));
        subscriptionEntry.setSubscriptionKey(object.optString("subscriptionKey"));

        result.setSubscriptionEntry(subscriptionEntry);
    }

}
