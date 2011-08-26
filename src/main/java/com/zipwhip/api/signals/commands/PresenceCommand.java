package com.zipwhip.api.signals.commands;

import com.zipwhip.api.signals.SignalsUtil;
import com.zipwhip.signals.presence.Presence;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/24/11
 * Time: 4:28 PM
 */
public class PresenceCommand extends SerializingCommand {

    public static final String ACTION = "presence";

    private Presence presence;

    /**
     * Create a new PresenceCommand
     *
     * @param presence JSONObject representing the signal
     */
    public PresenceCommand(Presence presence) {
        this.presence = presence;
    }

    public Presence getPresence() {
        return presence;
    }

    @Override
    public String serialize() {
        return SignalsUtil.serializePresence(presence);
    }

    @Override
    public String toString() {
        return serialize();
    }

}
