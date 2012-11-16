package com.zipwhip.api.connection;

import com.zipwhip.api.request.RequestBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 11/14/12
 * Time: 11:20 AM
 */
public class ParameterizedRequest implements RequestBody {

    private final Map<String, Object> params;

    public ParameterizedRequest(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public InputStream toStream() {
        RequestBuilder rb = new RequestBuilder();

        rb.params(params, true);

        return new ByteArrayInputStream(rb.build().getBytes());
    }
}
