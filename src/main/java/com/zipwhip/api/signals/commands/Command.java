package com.zipwhip.api.signals.commands;

import com.zipwhip.api.signals.VersionMapEntry;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/12/11 Time: 5:43 PM
 * 
 * This represents a command that the SignalServer will respond to.
 */
public abstract class Command {

    VersionMapEntry version;

    public VersionMapEntry getVersion() {
        return version;
    }

    public void setVersion(VersionMapEntry version) {
        this.version = version;
    }

}
