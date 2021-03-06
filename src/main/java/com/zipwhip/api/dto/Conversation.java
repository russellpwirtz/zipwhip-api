package com.zipwhip.api.dto;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: 7/5/11
 * Time: 8:16 PM
 * <p/>
 * Represents a Conversation with someone on Zipwhip.
 */
public class Conversation extends BasicDto {

    private static final long serialVersionUID = 5874121954954565L;

    long id;
    long deviceId;
    String deviceAddress;
    String fingerprint;
    String address;
    String cc;
    String bcc;
    int unreadCount;
    long lastContactId;
    boolean isNew;
    boolean deleted;
    long lastContactDeviceId;
    String lastContactFirstName;
    String lastContactLastName;
    Date lastMessageDate;
    Date lastNonDeletedMessageDate;
    String lastContactMobileNumber;
    String lastMessageBody;

    public String getLastMessageBody() {
        return lastMessageBody;
    }

    public void setLastMessageBody(String lastMessageBody) {
        this.lastMessageBody = lastMessageBody;
    }

    public String getLastContactMobileNumber() {
        return lastContactMobileNumber;
    }

    public void setLastContactMobileNumber(String lastContactMobileNumber) {
        this.lastContactMobileNumber = lastContactMobileNumber;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getBcc() {
        return bcc;
    }

    public void setBcc(String bcc) {
        this.bcc = bcc;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getLastContactId() {
        return lastContactId;
    }

    public void setLastContactId(long lastContactId) {
        this.lastContactId = lastContactId;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public long getLastContactDeviceId() {
        return lastContactDeviceId;
    }

    public void setLastContactDeviceId(long lastContactDeviceId) {
        this.lastContactDeviceId = lastContactDeviceId;
    }

    public String getLastContactFirstName() {
        return lastContactFirstName;
    }

    public void setLastContactFirstName(String lastContactFirstName) {
        this.lastContactFirstName = lastContactFirstName;
    }

    public String getLastContactLastName() {
        return lastContactLastName;
    }

    public void setLastContactLastName(String lastContactLastName) {
        this.lastContactLastName = lastContactLastName;
    }

    public Date getLastMessageDate() {
        return lastMessageDate;
    }

    public void setLastMessageDate(Date lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    public Date getLastNonDeletedMessageDate() {
        return lastNonDeletedMessageDate;
    }

    public void setLastNonDeletedMessageDate(Date lastNonDeletedMessageDate) {
        this.lastNonDeletedMessageDate = lastNonDeletedMessageDate;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder("==> Conversation details:");
        toStringBuilder.append("\nId: ").append(id);
        toStringBuilder.append("\nDeviceId: ").append(deviceId);
        toStringBuilder.append("\nDeviceAddress: ").append(deviceAddress);
        toStringBuilder.append("\nFingerprint: ").append(fingerprint);
        toStringBuilder.append("\nAddress: ").append(address);
        toStringBuilder.append("\nCc: ").append(cc);
        toStringBuilder.append("\nBcc: ").append(bcc);
        toStringBuilder.append("\nUnreadCount: ").append(unreadCount);
        toStringBuilder.append("\nDeviceId: ").append(deviceId);
        toStringBuilder.append("\nLastContactId: ").append(lastContactId);
        toStringBuilder.append("\nIsNew: ").append(isNew);
        toStringBuilder.append("\nDeleted: ").append(deleted);
        toStringBuilder.append("\nVersion: ").append(this.getVersion());
        toStringBuilder.append("\nLastContactDeviceId: ").append(lastContactDeviceId);
        toStringBuilder.append("\nLastContactFirstName: ").append(lastContactFirstName);
        toStringBuilder.append("\nLastContactLastName: ").append(lastContactLastName);
        toStringBuilder.append("\nLastUpdated: ").append(this.getLastUpdated());
        toStringBuilder.append("\nDateCreated: ").append(this.getDateCreated());
        toStringBuilder.append("\nLastMessageDate: ").append(lastMessageDate);
        toStringBuilder.append("\nLastNonDeletedMessageDate: ").append(lastNonDeletedMessageDate);
        toStringBuilder.append("\nLastContactMobileNumber: ").append(lastContactMobileNumber);
        toStringBuilder.append("\nLastMessageBodyHashCode: ").append(lastMessageBody.hashCode());

        return toStringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conversation)) return false;
        if (!super.equals(o)) return false;

        Conversation that = (Conversation) o;

        if (deleted != that.deleted) return false;
        if (deviceId != that.deviceId) return false;
        if (id != that.id) return false;
        if (isNew != that.isNew) return false;
        if (lastContactDeviceId != that.lastContactDeviceId) return false;
        if (lastContactId != that.lastContactId) return false;
        if (unreadCount != that.unreadCount) return false;
        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        if (bcc != null ? !bcc.equals(that.bcc) : that.bcc != null) return false;
        if (cc != null ? !cc.equals(that.cc) : that.cc != null) return false;
        if (deviceAddress != null ? !deviceAddress.equals(that.deviceAddress) : that.deviceAddress != null) return false;
        if (fingerprint != null ? !fingerprint.equals(that.fingerprint) : that.fingerprint != null) return false;
        if (lastContactFirstName != null ? !lastContactFirstName.equals(that.lastContactFirstName) : that.lastContactFirstName != null) return false;
        if (lastContactLastName != null ? !lastContactLastName.equals(that.lastContactLastName) : that.lastContactLastName != null) return false;
        if (lastContactMobileNumber != null ? !lastContactMobileNumber.equals(that.lastContactMobileNumber) : that.lastContactMobileNumber != null)
            return false;
        if (lastMessageBody != null ? !lastMessageBody.equals(that.lastMessageBody) : that.lastMessageBody != null) return false;
        if (lastMessageDate != null ? !lastMessageDate.equals(that.lastMessageDate) : that.lastMessageDate != null) return false;
        if (lastNonDeletedMessageDate != null ? !lastNonDeletedMessageDate.equals(that.lastNonDeletedMessageDate) : that.lastNonDeletedMessageDate != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (deviceId ^ (deviceId >>> 32));
        result = 31 * result + (deviceAddress != null ? deviceAddress.hashCode() : 0);
        result = 31 * result + (fingerprint != null ? fingerprint.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (cc != null ? cc.hashCode() : 0);
        result = 31 * result + (bcc != null ? bcc.hashCode() : 0);
        result = 31 * result + unreadCount;
        result = 31 * result + (int) (lastContactId ^ (lastContactId >>> 32));
        result = 31 * result + (isNew ? 1 : 0);
        result = 31 * result + (deleted ? 1 : 0);
        result = 31 * result + (int) (lastContactDeviceId ^ (lastContactDeviceId >>> 32));
        result = 31 * result + (lastContactFirstName != null ? lastContactFirstName.hashCode() : 0);
        result = 31 * result + (lastContactLastName != null ? lastContactLastName.hashCode() : 0);
        result = 31 * result + (lastMessageDate != null ? lastMessageDate.hashCode() : 0);
        result = 31 * result + (lastNonDeletedMessageDate != null ? lastNonDeletedMessageDate.hashCode() : 0);
        result = 31 * result + (lastContactMobileNumber != null ? lastContactMobileNumber.hashCode() : 0);
        result = 31 * result + (lastMessageBody != null ? lastMessageBody.hashCode() : 0);
        return result;
    }
}
