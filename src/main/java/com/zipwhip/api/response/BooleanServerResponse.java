package com.zipwhip.api.response;

import com.zipwhip.api.signals.Signal;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public class BooleanServerResponse extends ServerResponse {

    private boolean response;

    public BooleanServerResponse(String raw, boolean success, boolean response) {
        super(new ByteArrayInputStream(raw.getBytes()), success);

        this.response = response;
    }

    public boolean getResponse() {
        return response;
    }

}
