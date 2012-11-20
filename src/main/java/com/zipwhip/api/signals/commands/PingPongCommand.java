package com.zipwhip.api.signals.commands;

import com.zipwhip.signals.message.Action;
import com.zipwhip.util.StringUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/30/11
 * Time: 1:10 PM
 */
public class PingPongCommand extends SerializingCommand {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PingPongCommand.class);
    private static PingPongCommand shortformInstance;

    public static final Action ACTION = Action.PING; // "ping";

    private boolean isShortForm;
    private boolean request;
    private String token;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

            if (StringUtil.exists(token)) {
                json.put("token", token);
            }

        } catch (JSONException e) {
            LOGGER.error("Error serializing PingPongCommand", e);
        }

        return json.toString();
    }

    @Override
    public Action getAction() {
        return ACTION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PingPongCommand)) return false;

        PingPongCommand that = (PingPongCommand) o;

        if (isShortForm != that.isShortForm) return false;
        if (request != that.request) return false;
        if (timestamp != that.timestamp) return false;
        if (token != null ? !token.equals(that.token) : that.token != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (isShortForm ? 1 : 0);
        result = 31 * result + (request ? 1 : 0);
        result = 31 * result + (token != null ? token.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
