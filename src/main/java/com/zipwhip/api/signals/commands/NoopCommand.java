package com.zipwhip.api.signals.commands;

/**
 * A bidirectional NOOP command. 
 * <p/>
 * {action:NOOP}
 * 
 * @author jed
 *
 */
public class NoopCommand extends SerializingCommand<NoopCommand> {

    public static final String ACTION = "noop";

    @Override
    public String serialize(NoopCommand item) {
        return "{action:'NOOP'}";
    }
               
}
