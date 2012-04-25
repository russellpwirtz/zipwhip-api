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

}
