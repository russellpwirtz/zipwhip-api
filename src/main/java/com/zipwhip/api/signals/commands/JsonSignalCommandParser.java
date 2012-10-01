package com.zipwhip.api.signals.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.zipwhip.api.signals.JsonSignalParser;
import com.zipwhip.signals.PresenceUtil;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.signals.message.Action;
import com.zipwhip.util.Parser;
import com.zipwhip.util.StringUtil;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/3/11 Time: 5:20 PM
 * <p/>
 * Parse out a SignalCommand from a String.
 */
public class JsonSignalCommandParser implements Parser<String, Command<?>> {

	private static final Logger LOGGER = Logger.getLogger(JsonSignalCommandParser.class);

	private final Map<Action, Parser<JSONObject, Command<?>>> parsers;
	private final JsonSignalParser signalContentParser = new JsonSignalParser();

	public JsonSignalCommandParser() {

		parsers = new HashMap<Action, Parser<JSONObject, Command<?>>>();

		parsers.put(ConnectCommand.ACTION, CONNECT_PARSER);
		parsers.put(DisconnectCommand.ACTION, DISCONNECT_PARSER);
		parsers.put(SubscriptionCompleteCommand.ACTION, SUBSCRIPTION_COMPLETE_PARSER);
		parsers.put(SignalCommand.ACTION, SIGNAL_PARSER);
		parsers.put(PresenceCommand.ACTION, PRESENCE_PARSER);
		parsers.put(SignalVerificationCommand.ACTION, SIGNAL_VERIFICATION_PARSER);
		parsers.put(PingPongCommand.ACTION, PING_PONG_PARSER);
		parsers.put(NoopCommand.ACTION, NOOP_PARSER);
	}

	@Override
	public Command<?> parse(String string) throws Exception {

		// First check if it is a short form PING/PONG command
		if (PingPongCommand.getShortformInstance().serialize().equals(string)) {
			return PingPongCommand.getShortformInstance();
		}

		JSONObject json = new JSONObject(string);

		String action = json.optString("action");

		if (action != null && !StringUtil.EMPTY_STRING.equals(action)) {
			action = action.toLowerCase();
		} else {
            // No action, assume its a JSON style PONG
            return PingPongCommand.getShortformInstance();
        }

		Action valueOf = Action.valueOf(action.toUpperCase());
		Parser<JSONObject, Command<?>> parser = parsers.get(valueOf);

		if (parser == null) {
			throw new RuntimeException("No parser for " + action + " was found.");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Parsing" + JsonSignalParser.hashMessageBody(string));
		}

		return parser.parse(json);
	}

	public final Parser<JSONObject, Command<?>> CONNECT_PARSER = new Parser<JSONObject, Command<?>>() {
		@Override
		public Command<?> parse(JSONObject object) throws Exception {

			String clientId = object.optString("clientId");
			if(clientId == null || clientId.length() < 18) {
				Throwable t = new Throwable();
				LOGGER.fatal(t);
			}

			return new ConnectCommand(clientId);
		}
	};

	public final Parser<JSONObject, Command<?>> DISCONNECT_PARSER = new Parser<JSONObject, Command<?>>() {
		@Override
		public Command<?> parse(JSONObject object) throws Exception {

			String host = object.optString("host", StringUtil.EMPTY_STRING);
			int port = object.optInt("port", 0);
			int reconnectDelay = object.optInt("reconnectDelay", 0);
			boolean stop = object.optBoolean("stop", false);
			boolean ban = object.optBoolean("ban", false);

			return new DisconnectCommand(host, port, reconnectDelay, stop, ban);
		}
	};

	public final Parser<JSONObject, Command<?>> SUBSCRIPTION_COMPLETE_PARSER = new Parser<JSONObject, Command<?>>() {
		@Override
		public Command<?> parse(JSONObject object) throws Exception {

			if (!object.has("channels")) {
				LOGGER.warn("SUBSCRIPTION_COMPLETE command received with no channels.");
				return null;
			}

			List<Object> channels = new ArrayList<Object>();

			JSONArray channelArray = object.optJSONArray("channels");

			if (channelArray == null) {

				LOGGER.warn("SUBSCRIPTION_COMPLETE command received with no channels.");

			} else {

				for (int i = 0; i < channelArray.length(); i++) {
					channels.add(channelArray.get(i));
				}
			}

			String subscriptionId = object.optString("subscriptionId");

			SubscriptionCompleteCommand subscriptionCompleteCommand = new SubscriptionCompleteCommand(subscriptionId, channels);

			subscriptionCompleteCommand.setVersion(new VersionMapEntry(object.optString("versionKey", StringUtil.EMPTY_STRING), object.optLong("version", -1)));

			return subscriptionCompleteCommand;
		}
	};

	public final Parser<JSONObject, Command<?>> SIGNAL_PARSER = new Parser<JSONObject, Command<?>>() {
		@Override
		public Command<?> parse(JSONObject object) throws Exception {

			if (!object.has("signal")) {
				LOGGER.warn("SIGNAL command received with no signal object.");
				return null;
			}

			SignalCommand signalCommand = new SignalCommand(signalContentParser.parseSignal(object));
			signalCommand.setVersion(new VersionMapEntry(object.optString("versionKey", StringUtil.EMPTY_STRING), object.optLong("version", -1)));
			signalCommand.setBackfill(object.optBoolean("isBackfill", false));
			signalCommand.setMaxBackfillVersion(object.optLong("maxBackfillVersion", SignalCommand.NOT_BACKFILL_SIGNAL_VERSION));

			return signalCommand;
		}
	};

	public final Parser<JSONObject, Command<?>> PRESENCE_PARSER = new Parser<JSONObject, Command<?>>() {
		@Override
		public Command<?> parse(JSONObject object) throws Exception {

			if (!object.has("PRESENCE")) {
				LOGGER.warn("PRESENCE command received with no presence object.");
				return null;
			}

			PresenceCommand presenceCommand = new PresenceCommand(PresenceUtil.getInstance().parse(object.optJSONArray("PRESENCE")));
			presenceCommand.setVersion(new VersionMapEntry(object.optString("versionKey", StringUtil.EMPTY_STRING), object.optLong("version", -1)));

			return presenceCommand;
		}
	};

	public final Parser<JSONObject, Command<?>> SIGNAL_VERIFICATION_PARSER = new Parser<JSONObject, Command<?>>() {
		@Override
		public Command<?> parse(JSONObject object) throws Exception {
			return new SignalVerificationCommand();
		}
	};

	public final Parser<JSONObject, Command<?>> PING_PONG_PARSER = new Parser<JSONObject, Command<?>>() {
		@Override
		public Command<?> parse(JSONObject object) throws Exception {

			PingPongCommand pingPongCommand = PingPongCommand.getNewLongformInstance();
			pingPongCommand.setTimestamp(object.optLong("timestamp", 0));
			pingPongCommand.setRequest(object.optBoolean("request", false));
			pingPongCommand.setToken(object.optString("token", StringUtil.EMPTY_STRING));

			return pingPongCommand;
		}
	};

	public final Parser<JSONObject, Command<?>> NOOP_PARSER = new Parser<JSONObject, Command<?>>() {
		@Override
		public Command<?> parse(JSONObject object) throws Exception {
			return new NoopCommand();
		}
	};

}
