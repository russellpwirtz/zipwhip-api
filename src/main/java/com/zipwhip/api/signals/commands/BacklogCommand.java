package com.zipwhip.api.signals.commands;

import org.json.JSONObject;

import java.util.List;

/**
 * @author jed
 * 
 */
public class BacklogCommand extends Command {

    public static final String ACTION = "backlog";

    private List<JSONObject> messages;

    public BacklogCommand(List<JSONObject> messages) {
        this.messages = messages;
    }

    public List<JSONObject> getMessages() {
        return messages;
    }

}
