package com.zipwhip.api.signals.commands;

import java.util.List;

import com.zipwhip.signals.message.Action;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author jdinsel
 *
 */
public class BackfillCommand extends SerializingCommand<Long> {

	private static final long serialVersionUID = 1L;

	public static final Action ACTION = Action.BACKFILL; // "backfill";

    private static Logger LOGGER = Logger.getLogger(BackfillCommand.class);

    private String channel;

    /**
     * This command can be used in the following ways
     *
     * 1. Start and end versions
     * 2. Just start version
     * 3. Channel to get signals from (versionKey) channel
     *
     * @param command A list of version where [0] is start version ans [1] is (optionally) end version
     * @param channel The channel you wish to receive signals for
     */
	public BackfillCommand(List<Long> command, String channel) {
		this.command = command;
        this.channel = channel;
	}

    @Override
    public String serialize() {

        JSONObject json = new JSONObject();

        try {

            json.put("action", ACTION);

            if (StringUtil.exists(channel)) {
                json.put("channel", channel);
            }

            if (!CollectionUtil.isNullOrEmpty(command)) {
                json.put("commands", new JSONArray(command));
            }

        } catch (JSONException e) {
            LOGGER.error("Error serializing BackfillCommand", e);
        }

        return json.toString();
    }

    @Override
	public Action getAction() {
		return ACTION;
	}

}
