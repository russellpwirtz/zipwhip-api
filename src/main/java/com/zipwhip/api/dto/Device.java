package com.zipwhip.api.dto;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 17, 2009
 * Time: 7:57:04 PM
 */
public class Device extends BasicDto {

    private static final long serialVersionUID = 5876721954952365L;

    long id;
    String uuid;
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
        toStringBuilder.append("\nVersion: ").append(this.getVersion());
        toStringBuilder.append("\nUuid: ").append(uuid);
        toStringBuilder.append("\nDateCreated: ").append(this.getDateCreated());
        toStringBuilder.append("\nLastUpdated: ").append(this.getLastUpdated());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device)) return false;
        if (!super.equals(o)) return false;

        Device device = (Device) o;

        if (deviceNumber != device.deviceNumber) return false;
        if (id != device.id) return false;
        if (memberCount != device.memberCount) return false;
        if (userId != device.userId) return false;
        if (address != null ? !address.equals(device.address) : device.address != null) return false;
        if (channel != null ? !channel.equals(device.channel) : device.channel != null) return false;
        if (displayName != null ? !displayName.equals(device.displayName) : device.displayName != null) return false;
        if (textline != null ? !textline.equals(device.textline) : device.textline != null) return false;
        if (thread != null ? !thread.equals(device.thread) : device.thread != null) return false;
        if (uuid != null ? !uuid.equals(device.uuid) : device.uuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + (textline != null ? textline.hashCode() : 0);
        result = 31 * result + (int) (userId ^ (userId >>> 32));
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (int) (deviceNumber ^ (deviceNumber >>> 32));
        result = 31 * result + (int) (memberCount ^ (memberCount >>> 32));
        result = 31 * result + (thread != null ? thread.hashCode() : 0);
        return result;
    }
}
