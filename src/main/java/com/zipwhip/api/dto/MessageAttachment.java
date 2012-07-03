package com.zipwhip.api.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 4/24/12
 * Time: 4:09 PM
 */
public class MessageAttachment implements Serializable {

    private static final long serialVersionUID = 41254789521145474L;

    private Date dateCreated;
    private long deviceId;
    private long id;
    private Date lastUpdated;
    private long messageId;
    private String mimeType;
    private boolean isNew;
    private String storageKey;
    private long version;

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder("==> MessageAttachment details:");
        toStringBuilder.append("\ndateCreated: ").append(dateCreated);
        toStringBuilder.append("\ndeviceId: ").append(deviceId);
        toStringBuilder.append("\nid: ").append(id);
        toStringBuilder.append("\nlastUpdated: ").append(lastUpdated);
        toStringBuilder.append("\nmessageId: ").append(messageId);
        toStringBuilder.append("\nisNew: ").append(isNew);
        toStringBuilder.append("\nstorageKey: ").append(storageKey);
        toStringBuilder.append("\nversion: ").append(version);
        return toStringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageAttachment)) return false;

        MessageAttachment that = (MessageAttachment) o;

        if (deviceId != that.deviceId) return false;
        if (id != that.id) return false;
        if (isNew != that.isNew) return false;
        if (messageId != that.messageId) return false;
        if (version != that.version) return false;
        if (dateCreated != null ? !dateCreated.equals(that.dateCreated) : that.dateCreated != null) return false;
        if (lastUpdated != null ? !lastUpdated.equals(that.lastUpdated) : that.lastUpdated != null) return false;
        if (mimeType != null ? !mimeType.equals(that.mimeType) : that.mimeType != null) return false;
        if (storageKey != null ? !storageKey.equals(that.storageKey) : that.storageKey != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dateCreated != null ? dateCreated.hashCode() : 0;
        result = 31 * result + (int) (deviceId ^ (deviceId >>> 32));
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + (lastUpdated != null ? lastUpdated.hashCode() : 0);
        result = 31 * result + (int) (messageId ^ (messageId >>> 32));
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        result = 31 * result + (isNew ? 1 : 0);
        result = 31 * result + (storageKey != null ? storageKey.hashCode() : 0);
        result = 31 * result + (int) (version ^ (version >>> 32));
        return result;
    }
}
