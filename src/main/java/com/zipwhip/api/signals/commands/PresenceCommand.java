package com.zipwhip.api.signals.commands;

import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/24/11
 * Time: 4:28 PM
 */
public class PresenceCommand extends SerializingCommand<PresenceCommand> {

    public static final String ACTION = "presence";

    private JSONObject presence;

    /**
     * Create a new PresenceCommand
     *
     * @param presence JSONObject representing the signal
     */
    public PresenceCommand(JSONObject presence) {
        this.presence = presence;
    }

    public JSONObject getPresence() {
        return presence;
    }

    @Override
    public String serialize(PresenceCommand item) {
        // TODO serialize
        return null;
    }
}
