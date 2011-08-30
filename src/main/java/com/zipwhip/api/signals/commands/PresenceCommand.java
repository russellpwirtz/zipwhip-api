package com.zipwhip.api.signals.commands;

import com.zipwhip.api.signals.PresenceUtil;
import com.zipwhip.signals.presence.Presence;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/24/11
 * Time: 4:28 PM
 */
public class PresenceCommand extends SerializingCommand {

    public static final String ACTION = "presence";

        private static Logger logger = Logger.getLogger(PresenceCommand.class);

    private List<Presence> presence;

    /**
     * Create a new PresenceCommand
     *
     * @param presence JSONObject representing the signal
     */
    public PresenceCommand(List<Presence> presence) {
        this.presence = presence;
    }

    public List<Presence> getPresence() {
        return presence;
    }

    @Override
    public String serialize() {

        JSONObject json = new JSONObject();

        try {
            json.put(ACTION, PresenceUtil.getInstance().serialize(presence));
        } catch (JSONException e) {
            logger.error("Error serializing PresenceCommand", e);
        }

        return json.toString();
    }

    @Override
    public String toString() {
        return serialize();
    }

}
