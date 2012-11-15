package com.zipwhip.api.response;

import com.zipwhip.api.signals.Signal;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 18, 2009
 * Time: 10:37:20 AM
 * <p/>
 * Represents a server response that was null.
 */
public class NullServerResponse extends ServerResponse {

    public NullServerResponse(String raw, boolean success) {
        super(new ByteArrayInputStream(raw.getBytes()), success);
    }

}
