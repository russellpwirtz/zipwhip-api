package com.zipwhip.api.signals.commands;

import com.zipwhip.api.signals.PresenceUtil;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/2/11 Time: 11:28 AM
 * <p/>
 * for the {action:CONNECT} command
 */
public class ConnectCommand extends SerializingCommand {

    public static final String ACTION = "connect";

    private static Logger LOGGER = Logger.getLogger(ConnectCommand.class);

    private String clientId;
    private Map<String, Long> versions;
    private Presence presence;

    public ConnectCommand(String clientId) {
        this(clientId, null, null);
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

    public String toString() {
        return serialize();
    }

    @Override
    public String serialize() {

        JSONObject json = new JSONObject();

        try {

            json.put("action", "CONNECT");

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

}
