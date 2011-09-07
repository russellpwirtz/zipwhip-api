package com.zipwhip.api.dto;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 17, 2009
 * Time: 7:57:04 PM
 */
public class Device {

    long id;
    String uuid;
    int version;
    Date dateCreated;
    Date lastUpdated;
    String channel;
    String textline;
    long userId;
    String displayName;
    String address;
    long deviceNumber;
    long memberCount;
    String thread;

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTextline() {
        return textline;
    }

    public void setTextline(String textline) {
        this.textline = textline;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getDeviceNumber() {
        return deviceNumber;
    }

    public void setDeviceNumber(long deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(long memberCount) {
        this.memberCount = memberCount;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder("==> Device details:");
        toStringBuilder.append("\nId: ").append(id);
        toStringBuilder.append("\nVersion: ").append(version);
        toStringBuilder.append("\nUuid: ").append(uuid);
        toStringBuilder.append("\nDateCreated: ").append(dateCreated);
        toStringBuilder.append("\nLastUpdated: ").append(lastUpdated);
        toStringBuilder.append("\nChannel: ").append(channel);
        toStringBuilder.append("\nTextline: ").append(textline);
        toStringBuilder.append("\nUserId: ").append(userId);
        toStringBuilder.append("\nDisplayName: ").append(displayName);
        toStringBuilder.append("\nAddress: ").append(address);
        toStringBuilder.append("\nDeviceNumber: ").append(deviceNumber);
        toStringBuilder.append("\nMemberCount: ").append(memberCount);
        toStringBuilder.append("\nThread: ").append(thread);

        return toStringBuilder.toString();
    }

}
