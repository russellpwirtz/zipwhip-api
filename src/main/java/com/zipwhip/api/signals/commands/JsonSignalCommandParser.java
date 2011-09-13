package com.zipwhip.api.signals.commands;

import com.zipwhip.api.signals.JsonSignalParser;
import com.zipwhip.api.signals.PresenceUtil;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.api.signals.sockets.netty.StringToChannelBuffer;
import com.zipwhip.util.Parser;
import com.zipwhip.util.StringUtil;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/3/11 Time: 5:20 PM
 * <p/>
 * Parse out a SignalCommand from a String.
 */
public class JsonSignalCommandParser implements Parser<String, Command> {

    private Map<String, Parser<JSONObject, Command>> parsers;
    
    private static final Logger LOGGER = Logger.getLogger(JsonSignalCommandParser.class);

    public JsonSignalCommandParser() {

        parsers = new HashMap<String, Parser<JSONObject, Command>>();

        parsers.put(ConnectCommand.ACTION, CONNECT_PARSER);
        parsers.put(DisconnectCommand.ACTION, DISCONNECT_PARSER);
        parsers.put(SubscriptionCompleteCommand.ACTION, SUBSCRIPTION_COMPLETE_PARSER);
        parsers.put(BacklogCommand.ACTION, BACKLOG_PARSER);
        parsers.put(SignalCommand.ACTION, SIGNAL_PARSER);
        parsers.put(PresenceCommand.ACTION, PRESENCE_PARSER);
        parsers.put(SignalVerificationCommand.ACTION, SIGNAL_VERIFICATION_PARSER);
        parsers.put(NoopCommand.ACTION, NOOP_PARSER);
    }

    @Override
    public Command parse(String string) throws Exception {

        // First check if it is a PONG
        if (StringToChannelBuffer.CRLF.equals(string)) {
            return PingPongCommand.getInstance();
        }

        JSONObject json = new JSONObject(string);

        String action = json.optString("action");

        if (action != null) {
            action = action.toLowerCase();
        }

        Parser<JSONObject, Command> parser = parsers.get(action);

        if (parser == null) {
            throw new RuntimeException("No parser for " + action + " was found.");
        }

        LOGGER.debug("Parsing" + string);

        return parser.parse(json);
    }
    
    private static final Parser<JSONObject, Command> CONNECT_PARSER = new Parser<JSONObject, Command>() {
        @Override
        public Command parse(JSONObject object) throws Exception {
            
            String clientId = object.optString("clientId");

            return new ConnectCommand(clientId);
        }
    };
    
    private static final Parser<JSONObject, Command> DISCONNECT_PARSER = new Parser<JSONObject, Command>() {
        @Override
        public Command parse(JSONObject object) throws Exception {

            String host = object.optString("host", StringUtil.EMPTY_STRING);
            int port = object.optInt("port", 0);
            int reconnectDelay = object.optInt("reconnectDelay", 0);
            boolean stop = object.optBoolean("stop", false);
            
            return new DisconnectCommand(host, port, reconnectDelay, stop);
        }
    };

    private static final Parser<JSONObject, Command> SUBSCRIPTION_COMPLETE_PARSER = new Parser<JSONObject, Command>() {
        @Override
        public Command parse(JSONObject object) throws Exception {

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
        
    private static final Parser<JSONObject, Command> BACKLOG_PARSER = new Parser<JSONObject, Command>() {
        @Override
        public Command parse(JSONObject object) throws Exception {

            if (!object.has("messages")) {
                LOGGER.warn("BACKLOG command received with no messages.");
                return null;
            }

            JSONArray messages = object.optJSONArray("messages");
            if (messages == null) {
                LOGGER.warn("BACKLOG command received with no messages.");
                return null;
            }

            List<SignalCommand> signalCommands = new ArrayList<SignalCommand>();

            for (int i = 0; i < messages.length(); i++) {

                JSONObject signalJson = messages.optJSONObject(i);

                if (signalJson != null && signalJson.has("signal")) {
                    signalCommands.add((SignalCommand) SIGNAL_PARSER.parse(signalJson));
                }
            }

            return new BacklogCommand(signalCommands);
        }
    };

    private static final Parser<JSONObject, Command> SIGNAL_PARSER = new Parser<JSONObject, Command>() {
        @Override
        public Command parse(JSONObject object) throws Exception {

            if (!object.has("signal")) {
                LOGGER.warn("SIGNAL command received with no signal object.");
                return null;
            }

            SignalCommand signalCommand = new SignalCommand(JsonSignalParser.getInstance().parseSignal(object));
            signalCommand.setVersion(new VersionMapEntry(object.optString("versionKey", StringUtil.EMPTY_STRING), object.optLong("version", -1)));

            return signalCommand;
        }
    };

    private static final Parser<JSONObject, Command> PRESENCE_PARSER = new Parser<JSONObject, Command>() {
        @Override
        public Command parse(JSONObject object) throws Exception {

            if (!object.has("presence")) {
                LOGGER.warn("PRESENCE command received with no presence object.");
                return null;
            }

            PresenceCommand presenceCommand = new PresenceCommand(PresenceUtil.getInstance().parse(object.optJSONArray("presence")));
            presenceCommand.setVersion(new VersionMapEntry(object.optString("versionKey", StringUtil.EMPTY_STRING), object.optLong("version", -1)));

            return presenceCommand;
        }
    };
    
    private static final Parser<JSONObject, Command> SIGNAL_VERIFICATION_PARSER = new Parser<JSONObject, Command>() {
        @Override
        public Command parse(JSONObject object) throws Exception {
            return new SignalVerificationCommand();
        }
    };

    private static final Parser<JSONObject, Command> NOOP_PARSER = new Parser<JSONObject, Command>() {
        @Override
        public Command parse(JSONObject object) throws Exception {                       
            return new NoopCommand();
        }
    };

}
