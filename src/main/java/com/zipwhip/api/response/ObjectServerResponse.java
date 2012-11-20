package com.zipwhip.api.response;

import com.zipwhip.api.signals.Signal;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
 * Represents a JSON Object ServerResponse.
 */
public class ObjectServerResponse extends ServerResponse {

    public JSONObject response;

    public ObjectServerResponse(String raw, boolean success, JSONObject response) {
        super(new ByteArrayInputStream(raw.getBytes()), success);

        this.response = response;
    }

}
