package com.zipwhip.api.signals.commands;

import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.zipwhip.signals.message.Action;
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
	// This versions field is never used. in fact, it's not usable via a getter
	private final Map<String, Long> versions;

	public ConnectCommand(String clientId) {
        this(clientId, null);
	}

	public ConnectCommand(String clientId, Map<String, Long> versions) {
		this.clientId = clientId;
		this.versions = versions;
	}

	public boolean isSuccessful() {
		return StringUtil.exists(clientId);
	}

	public String getClientId() {
		return clientId;
	}

	@Override
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

		} catch (JSONException e) {
			LOGGER.error("Error serializing ConnectCommand", e);
		}

		return json.toString();
	}

	@Override
	public Action getAction() {
		return ACTION;
	}
}
