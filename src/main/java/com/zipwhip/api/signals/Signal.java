package com.zipwhip.api.signals;

import java.io.Serializable;

/**
 * A raw declaration of a signal. Most of the time you'd use a subclass of this to get the important details.
 */
public class Signal implements Serializable {

    private static final long serialVersionUID = 4312231478912373L;

    /**
     * Some signals can be parsed as DTO's. If that's possible it will be in the content.
     */
    Object content;
    String type;
    String scope;
    String uuid;
    String event;
    String reason;
    String uri;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getUri() {

        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Signal)) return false;

        Signal signal = (Signal) o;

        if (content != null ? !content.equals(signal.content) : signal.content != null) return false;
        if (event != null ? !event.equals(signal.event) : signal.event != null) return false;
        if (reason != null ? !reason.equals(signal.reason) : signal.reason != null) return false;
        if (scope != null ? !scope.equals(signal.scope) : signal.scope != null) return false;
        if (type != null ? !type.equals(signal.type) : signal.type != null) return false;
        if (uri != null ? !uri.equals(signal.uri) : signal.uri != null) return false;
        if (uuid != null ? !uuid.equals(signal.uuid) : signal.uuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = content != null ? content.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (event != null ? event.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        return result;
    }
}
