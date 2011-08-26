package com.zipwhip.api.signals.commands;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/12/11 Time: 5:43 PM
 * 
 * This represents a command that the SignalServer will respond to.
 */
public abstract class Command {

    String versionKey;
    Long version;

    public String getVersionKey() {
        return versionKey;
    }

    public void setVersionKey(String versionKey) {
        this.versionKey = versionKey;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
