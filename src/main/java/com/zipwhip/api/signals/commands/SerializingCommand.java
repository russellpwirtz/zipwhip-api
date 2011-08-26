package com.zipwhip.api.signals.commands;

import com.zipwhip.util.Serializer;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/25/11
 * Time: 2:04 PM
 *
 * Base class for SignalServer commands that are bidirectional.
 *
 */
public abstract class SerializingCommand<T extends SerializingCommand> extends Command implements Serializer<T> {

}
