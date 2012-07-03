package com.zipwhip.api.signals.commands;

import java.util.Collections;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.zipwhip.api.signals.PresenceUtil;
import com.zipwhip.signals.message.Action;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/2/11 Time: 11:28 AM
 * <p/>
 * for the {action:CONNECT} command
 */
public class ConnectCommand extends SerializingCommand {

    private static final long serialVersionUID = 1L;
    public static final Action ACTION = Action.CONNECT; // "connect";

    private static Logger LOGGER = Logger.getLogger(ConnectCommand.class);

    private final String clientId;
    private final Map<String, Long> versions;
    private final Presence presence;

    public ConnectCommand(String clientId) {
        this(clientId, null);
    }

    public ConnectCommand(String clientId, Map<String, Long> versions) {
        this(clientId, versions, null);
    }

    public ConnectCommand(String clientId, Map<String, Long> versions, Presence presence) {
        this.clientId = clientId;
        this.versions = versions;
        this.presence = presence;
    }

    public boolean isSuccessful() {
        return StringUtil.exists(clientId);
    }

    public String getClientId() {
        return clientId;
    }

    public final Presence getPresence() {
        return presence;
    }

    @Override
    public String toString() {
        return serialize();
    }

    @Override
    public String serialize() {

        JSONObject json = new JSONObject();

        try {

            json.put("action", ACTION);

            if (StringUtil.exists(clientId)) {
                json.put("clientId", clientId);
            }

            if (!CollectionUtil.isNullOrEmpty(versions)) {
                json.put("versions", new JSONObject(versions));
            }

            if (presence != null) {
                json.put("presence", PresenceUtil.getInstance().serialize(Collections.singletonList(presence)));
            }

        } catch (JSONException e) {
            LOGGER.error("Error serializing ConnectCommand", e);
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
        if (!(o instanceof ConnectCommand)) return false;

        ConnectCommand that = (ConnectCommand) o;

        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        if (presence != null ? !presence.equals(that.presence) : that.presence != null) return false;
        if (versions != null ? !versions.equals(that.versions) : that.versions != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = clientId != null ? clientId.hashCode() : 0;
        result = 31 * result + (versions != null ? versions.hashCode() : 0);
        result = 31 * result + (presence != null ? presence.hashCode() : 0);
        return result;
    }
}
