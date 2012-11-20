package com.zipwhip.api.response;

import com.zipwhip.api.signals.Signal;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Represents a ServerResponse that might be in some unknown format.
 *
 * We don't know if it's JSON or XML or binary encoded.
 */
public abstract class ServerResponse {

    private final boolean success;
    private final InputStream raw;

    protected ServerResponse(InputStream raw, boolean success) {
        this.raw = raw;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public InputStream getRaw() {
        return raw;
    }
}
