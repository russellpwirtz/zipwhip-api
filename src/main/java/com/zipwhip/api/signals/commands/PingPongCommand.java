package com.zipwhip.api.signals.commands;

import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/30/11
 * Time: 1:10 PM
 */
public class PingPongCommand extends SerializingCommand {

    private static final Logger LOGGER = Logger.getLogger(PingPongCommand.class);
    private static PingPongCommand shortformInstance;

    public static final String ACTION = "ping";

    private boolean isShortForm;
    private boolean request;
    private long timestamp;

    /**
     * Private constructor to enforce use of the static methods
     *
     * @param isShortForm True is we want the shortform (CRLF).
     */
    private PingPongCommand(boolean isShortForm) {
        this.isShortForm = isShortForm;
    }

    /**
     * Get a singleton instance of the shortform command.
     *
     * @return A singleton instance of the shortform command.
     */
    public static PingPongCommand getShortformInstance() {

        if (shortformInstance == null) {
            shortformInstance = new PingPongCommand(true);
        }

        return shortformInstance;
    }

    /**
     * Get an instance of the longform command.
     *
     * @return An instance of the longform command.
     */
    public static PingPongCommand getNewLongformInstance() {
        return new PingPongCommand(false);
    }

    public boolean isRequest() {
        return request;
    }

    public void setRequest(boolean request) {
        this.request = request;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String serialize() {

        if (isShortForm) {
            // CRLF is appended by StringToChannelBuffer, don't append here or we will be sending 2 PINGs
            return StringUtil.EMPTY_STRING;
        }

        JSONObject json = new JSONObject();

        try {
            json.put("action", "PONG");

            if (timestamp > 0) {
                json.put("timestamp", timestamp);
            }

        } catch (JSONException e) {
            LOGGER.error("Error serializing PingPongCommand", e);
        }

        return json.toString();
    }

}
