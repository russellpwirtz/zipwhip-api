package com.zipwhip.api.signals.commands;

import com.zipwhip.signals.presence.Presence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: msmyers
 * Date: 8/24/11
 * Time: 4:28 PM
 */
public class PresenceCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = LoggerFactory.getLogger(PresenceCommand.class);

    private Presence presence;

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
