package com.zipwhip.api.signals.commands;

import com.zipwhip.api.signals.Signal;

import java.util.List;

/**
 * @author jed
 * 
 */
public class BacklogCommand extends Command {

    public static final String ACTION = "backlog";

    private List<SignalCommand> command;

    public BacklogCommand(List<SignalCommand> command) {
        this.command = command;
    }

    public List<SignalCommand> getCommands() {
        return command;
    }

}
