package com.zipwhip.api.response;

import com.zipwhip.api.signals.Signal;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 18, 2009
 * Time: 10:23:38 AM
 *
 * Represents a server response of the form: {success:boolean, response: String}
 */
public class StringServerResponse extends ServerResponse {

    public String response;

    public StringServerResponse(String raw, boolean success, String response) {
        super(new ByteArrayInputStream(raw.getBytes()), success);

        this.response = response;
    }
}
