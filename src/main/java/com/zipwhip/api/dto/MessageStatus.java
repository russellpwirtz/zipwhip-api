package com.zipwhip.api.dto;

import java.io.Serializable;

/**
 * @author Me
 */
public class MessageStatus implements Serializable {

    private static final long serialVersionUID = 5874121954926865L;

    private boolean delivered;
    private long id;
    private String uuid;
    private String statusDescription;
    private int statusCode;

    public MessageStatus() {
    }

    public MessageStatus(Message message) {
        this.delivered = (message.getStatusCode() == 0 || message.getStatusCode() == 4);
        this.id = message.getId();
        this.uuid = message.getUuid();
        this.statusDescription = message.getStatusDesc();
        this.statusCode = message.getStatusCode();
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder("==> MessageStatus details:");
        toStringBuilder.append("\nDelivered: ").append(delivered);
        toStringBuilder.append("\nId: ").append(Long.toString(id));
        toStringBuilder.append("\nUUID: ").append(uuid);
        toStringBuilder.append("\nStatus Code: ").append(statusCode);
        toStringBuilder.append("\nStatus Description: ").append(statusDescription);
        return toStringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageStatus)) return false;

        MessageStatus that = (MessageStatus) o;

        if (delivered != that.delivered) return false;
        if (id != that.id) return false;
        if (statusCode != that.statusCode) return false;
        if (statusDescription != null ? !statusDescription.equals(that.statusDescription) : that.statusDescription != null) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (delivered ? 1 : 0);
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (statusDescription != null ? statusDescription.hashCode() : 0);
        result = 31 * result + statusCode;
        return result;
    }
}
