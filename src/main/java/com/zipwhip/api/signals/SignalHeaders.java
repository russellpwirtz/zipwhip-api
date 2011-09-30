package com.zipwhip.api.signals;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/19/11 Time: 4:38 PM
 * 
 * The SignalServer uses SignalHeaders to determine who the message was from
 * 
 */
public class SignalHeaders {

    private String versionKey;
    private Long version;
    private String action;
    private String channel;

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

}
