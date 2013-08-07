package com.zipwhip.api.dto;

/**
 * @author Ali Serghini
 *         Date: 8/6/13
 *         Time: 4:32 PM
 */
public class Group extends BasicDto {

    private static final long serialVersionUID = -3720922317121919087L;

    private String address;
    private int cachedContactsCount;
    private String channel;
    private long deviceId;
    private String displayName;
    private long dtoParentId;
    private long id;
    private long linkedDeviceId;
    private boolean newGroup;
    private String phoneKey;
    private String textline;
    private String thread;
    private String type;
    private long userId;
    private String uuid;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getCachedContactsCount() {
        return cachedContactsCount;
    }

    public void setCachedContactsCount(int cachedContactsCount) {
        this.cachedContactsCount = cachedContactsCount;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getDtoParentId() {
        return dtoParentId;
    }

    public void setDtoParentId(long dtoParentId) {
        this.dtoParentId = dtoParentId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLinkedDeviceId() {
        return linkedDeviceId;
    }

    public void setLinkedDeviceId(long linkedDeviceId) {
        this.linkedDeviceId = linkedDeviceId;
    }

    public boolean isNewGroup() {
        return newGroup;
    }

    public void setNewGroup(boolean newGroup) {
        this.newGroup = newGroup;
    }

    public String getPhoneKey() {
        return phoneKey;
    }

    public void setPhoneKey(String phoneKey) {
        this.phoneKey = phoneKey;
    }

    public String getTextline() {
        return textline;
    }

    public void setTextline(String textline) {
        this.textline = textline;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group)) return false;
        if (!super.equals(o)) return false;

        Group group = (Group) o;

        if (cachedContactsCount != group.cachedContactsCount) return false;
        if (deviceId != group.deviceId) return false;
        if (dtoParentId != group.dtoParentId) return false;
        if (id != group.id) return false;
        if (linkedDeviceId != group.linkedDeviceId) return false;
        if (newGroup != group.newGroup) return false;
        if (userId != group.userId) return false;
        if (address != null ? !address.equals(group.address) : group.address != null) return false;
        if (channel != null ? !channel.equals(group.channel) : group.channel != null) return false;
        if (displayName != null ? !displayName.equals(group.displayName) : group.displayName != null) return false;
        if (phoneKey != null ? !phoneKey.equals(group.phoneKey) : group.phoneKey != null) return false;
        if (textline != null ? !textline.equals(group.textline) : group.textline != null) return false;
        if (thread != null ? !thread.equals(group.thread) : group.thread != null) return false;
        if (type != null ? !type.equals(group.type) : group.type != null) return false;
        if (uuid != null ? !uuid.equals(group.uuid) : group.uuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + cachedContactsCount;
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + (int) (deviceId ^ (deviceId >>> 32));
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (int) (dtoParentId ^ (dtoParentId >>> 32));
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (linkedDeviceId ^ (linkedDeviceId >>> 32));
        result = 31 * result + (newGroup ? 1 : 0);
        result = 31 * result + (phoneKey != null ? phoneKey.hashCode() : 0);
        result = 31 * result + (textline != null ? textline.hashCode() : 0);
        result = 31 * result + (thread != null ? thread.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (int) (userId ^ (userId >>> 32));
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Group");
        sb.append("{address='").append(address).append('\'');
        sb.append(", cachedContactsCount=").append(cachedContactsCount);
        sb.append(", channel='").append(channel).append('\'');
        sb.append(", deviceId=").append(deviceId);
        sb.append(", displayName='").append(displayName).append('\'');
        sb.append(", dtoParentId=").append(dtoParentId);
        sb.append(", id=").append(id);
        sb.append(", linkedDeviceId=").append(linkedDeviceId);
        sb.append(", newGroup=").append(newGroup);
        sb.append(", phoneKey='").append(phoneKey).append('\'');
        sb.append(", textline='").append(textline).append('\'');
        sb.append(", thread='").append(thread).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", userId=").append(userId);
        sb.append(", uuid='").append(uuid).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
