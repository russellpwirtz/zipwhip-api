package com.zipwhip.api.signals.commands;

import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.StringUtil;

import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/2/11 Time: 11:28 AM
 * <p/>
 * for the {action:CONNECT} command
 */
public class ConnectCommand extends SerializingCommand {

    public static final String ACTION = "connect";

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

        // TODO Serialize these

        if (StringUtil.isNullOrEmpty(clientId)) {
            return "{'action':'CONNECT'}";
        } else {
            return "{'action':'CONNECT', 'clientId':'" + clientId + "'}";
        }
    }

}
