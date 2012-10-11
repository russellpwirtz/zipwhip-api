package com.zipwhip.api.signals.commands;

import com.zipwhip.signals.PresenceUtil;
import com.zipwhip.signals.message.Action;
import com.zipwhip.signals.presence.Presence;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/24/11
 * Time: 4:28 PM
 */
public class PresenceCommand extends SerializingCommand {

    private static final long serialVersionUID = 1L;

    public static final Action ACTION = Action.PRESENCE; // "presence";

    private static Logger logger = LoggerFactory.getLogger(PresenceCommand.class);

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
            json.put("class", PresenceCommand.class.getName());
            json.put("action", ACTION.name());
            json.put(ACTION.name(), PresenceUtil.getInstance().serialize(presence));
        } catch (JSONException e) {
            logger.error("Error serializing PresenceCommand", e);
        }

        return json.toString();
    }

    @Override
    public String toString() {
        return serialize();
    }

    @Override
    public Action getAction() {
        return ACTION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PresenceCommand)) return false;

        PresenceCommand that = (PresenceCommand) o;

        if (presence != null ? !presence.equals(that.presence) : that.presence != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return presence != null ? presence.hashCode() : 0;
    }
}
