package com.zipwhip.api.dto;

/**
 * Created by IntelliJ IDEA.
 * * Date: Jul 17, 2009
 * Time: 7:40:55 PM
 */
public class Message extends BasicDto {

    private static final long serialVersionUID = 5874121953591365L;

    /*
        These are fields in the JSON
        that are not parsed by signal
        clients.

        bodySize
        visible
        metaDataId
        dtoParentId
        scheduledDate
        openMarketMessageId
        class
        isParent
        loc
        messageConsoleLog
        isInFinalState
        encoded
        expectDeliveryReceipt
        transferedToCarrierReceipt
        parentId
        phoneKey
        smartForwarded
        isSelf
        deliveryReceipt
        dishedToOpenMarket
        creatorId
        smartForwardingCandidate
        DCSId
        latlong
        new
        UDH
        carbonedMessageId
     */

    long id;
    long contactId;
    long deviceId;
    TransmissionState transmissionState;
    String mobileNumber;
    String address;
    String destinationAddress;
    String sourceAddress;
    String direction;
    String body;
    String uuid;
    String from;
    String fromName;
    String advertisement;
    boolean read;
    int statusCode;
    String statusDesc;
    String messageType;
    String cc;
    String bcc;
    String fwd;
    String thread;
    String channel;
    String to;
    String carrier;
    String subject;
    String firstName;
    String lastName;
    boolean deleted;
    String errorDesc;
    boolean errorState;
    long contactDeviceId;
    String fingerprint;
    boolean hasAttachment;

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    public TransmissionState getTransmissionState() {
        return transmissionState;
    }

    public void setTransmissionState(TransmissionState transmissionState) {
        this.transmissionState = transmissionState;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getAdvertisement() {
        return advertisement;
    }

    public void setAdvertisement(String advertisement) {
        this.advertisement = advertisement;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
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

    public String getFwd() {
        return fwd;
    }

    public void setFwd(String fwd) {
        this.fwd = fwd;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public boolean isErrorState() {
        return errorState;
    }

    public void setErrorState(boolean errorState) {
        this.errorState = errorState;
    }

    public long getContactDeviceId() {
        return contactDeviceId;
    }

    public void setContactDeviceId(long contactDeviceId) {
        this.contactDeviceId = contactDeviceId;
    }

    public boolean isHasAttachment() {
        return hasAttachment;
    }

    public void setHasAttachment(boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder("==> Message details:");
        toStringBuilder.append("\nId: ").append(id);
        toStringBuilder.append("\nContactId: ").append(contactId);
        toStringBuilder.append("\nDeviceId: ").append(deviceId);
        toStringBuilder.append("\nMobileNumber: ").append(mobileNumber);
        toStringBuilder.append("\nAddress: ").append(address);
        toStringBuilder.append("\nDestinationAddress: ").append(destinationAddress);
        toStringBuilder.append("\nSourceAddress: ").append(sourceAddress);
        toStringBuilder.append("\ndirection: ").append(direction);
        toStringBuilder.append("\nBodyHashCode: ").append(body.hashCode());
        toStringBuilder.append("\nUuid: ").append(uuid);
        toStringBuilder.append("\nFrom: ").append(from);
        toStringBuilder.append("\nFromName: ").append(fromName);
        toStringBuilder.append("\nAdvertisement: ").append(advertisement);
        toStringBuilder.append("\nRead: ").append(read);
        toStringBuilder.append("\nStatusCode: ").append(statusCode);
        toStringBuilder.append("\nStatusDesc: ").append(statusDesc);
        toStringBuilder.append("\nMessageType: ").append(messageType);
        toStringBuilder.append("\nDateCreated: ").append(this.getDateCreated());
        toStringBuilder.append("\nLastUpdated: ").append(this.getLastUpdated());
        toStringBuilder.append("\nVersion: ").append(this.getVersion());
        toStringBuilder.append("\ncc: ").append(cc);
        toStringBuilder.append("\nbcc: ").append(bcc);
        toStringBuilder.append("\nfwd: ").append(fwd);
        toStringBuilder.append("\nthread: ").append(thread);
        toStringBuilder.append("\nchannel: ").append(channel);
        toStringBuilder.append("\nto: ").append(to);
        toStringBuilder.append("\ncarrier: ").append(carrier);
        toStringBuilder.append("\nsubject: ").append(subject);
        toStringBuilder.append("\nfirstName: ").append(firstName);
        toStringBuilder.append("\nlastName: ").append(lastName);
        toStringBuilder.append("\nDeleted: ").append(deleted);
        toStringBuilder.append("\nErrorDesc: ").append(errorDesc);
        toStringBuilder.append("\nErrorState: ").append(errorState);
        toStringBuilder.append("\nContactDeviceId: ").append(contactDeviceId);
        toStringBuilder.append("\nFingerprint: ").append(fingerprint);
        toStringBuilder.append("\nhasAttachment: ").append(hasAttachment);
        return toStringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        if (!super.equals(o)) return false;

        Message message = (Message) o;

        if (contactDeviceId != message.contactDeviceId) return false;
        if (contactId != message.contactId) return false;
        if (deleted != message.deleted) return false;
        if (deviceId != message.deviceId) return false;
        if (errorState != message.errorState) return false;
        if (hasAttachment != message.hasAttachment) return false;
        if (id != message.id) return false;
        if (read != message.read) return false;
        if (statusCode != message.statusCode) return false;
        if (address != null ? !address.equals(message.address) : message.address != null) return false;
        if (advertisement != null ? !advertisement.equals(message.advertisement) : message.advertisement != null) return false;
        if (bcc != null ? !bcc.equals(message.bcc) : message.bcc != null) return false;
        if (body != null ? !body.equals(message.body) : message.body != null) return false;
        if (carrier != null ? !carrier.equals(message.carrier) : message.carrier != null) return false;
        if (cc != null ? !cc.equals(message.cc) : message.cc != null) return false;
        if (channel != null ? !channel.equals(message.channel) : message.channel != null) return false;
        if (destinationAddress != null ? !destinationAddress.equals(message.destinationAddress) : message.destinationAddress != null) return false;
        if (direction != null ? !direction.equals(message.direction) : message.direction != null) return false;
        if (errorDesc != null ? !errorDesc.equals(message.errorDesc) : message.errorDesc != null) return false;
        if (fingerprint != null ? !fingerprint.equals(message.fingerprint) : message.fingerprint != null) return false;
        if (firstName != null ? !firstName.equals(message.firstName) : message.firstName != null) return false;
        if (from != null ? !from.equals(message.from) : message.from != null) return false;
        if (fromName != null ? !fromName.equals(message.fromName) : message.fromName != null) return false;
        if (fwd != null ? !fwd.equals(message.fwd) : message.fwd != null) return false;
        if (lastName != null ? !lastName.equals(message.lastName) : message.lastName != null) return false;
        if (messageType != null ? !messageType.equals(message.messageType) : message.messageType != null) return false;
        if (mobileNumber != null ? !mobileNumber.equals(message.mobileNumber) : message.mobileNumber != null) return false;
        if (sourceAddress != null ? !sourceAddress.equals(message.sourceAddress) : message.sourceAddress != null) return false;
        if (statusDesc != null ? !statusDesc.equals(message.statusDesc) : message.statusDesc != null) return false;
        if (subject != null ? !subject.equals(message.subject) : message.subject != null) return false;
        if (thread != null ? !thread.equals(message.thread) : message.thread != null) return false;
        if (to != null ? !to.equals(message.to) : message.to != null) return false;
        if (transmissionState != message.transmissionState) return false;
        if (uuid != null ? !uuid.equals(message.uuid) : message.uuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (contactId ^ (contactId >>> 32));
        result = 31 * result + (int) (deviceId ^ (deviceId >>> 32));
        result = 31 * result + (transmissionState != null ? transmissionState.hashCode() : 0);
        result = 31 * result + (mobileNumber != null ? mobileNumber.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (destinationAddress != null ? destinationAddress.hashCode() : 0);
        result = 31 * result + (sourceAddress != null ? sourceAddress.hashCode() : 0);
        result = 31 * result + (direction != null ? direction.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (from != null ? from.hashCode() : 0);
        result = 31 * result + (fromName != null ? fromName.hashCode() : 0);
        result = 31 * result + (advertisement != null ? advertisement.hashCode() : 0);
        result = 31 * result + (read ? 1 : 0);
        result = 31 * result + statusCode;
        result = 31 * result + (statusDesc != null ? statusDesc.hashCode() : 0);
        result = 31 * result + (messageType != null ? messageType.hashCode() : 0);
        result = 31 * result + (cc != null ? cc.hashCode() : 0);
        result = 31 * result + (bcc != null ? bcc.hashCode() : 0);
        result = 31 * result + (fwd != null ? fwd.hashCode() : 0);
        result = 31 * result + (thread != null ? thread.hashCode() : 0);
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (carrier != null ? carrier.hashCode() : 0);
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (deleted ? 1 : 0);
        result = 31 * result + (errorDesc != null ? errorDesc.hashCode() : 0);
        result = 31 * result + (errorState ? 1 : 0);
        result = 31 * result + (int) (contactDeviceId ^ (contactDeviceId >>> 32));
        result = 31 * result + (fingerprint != null ? fingerprint.hashCode() : 0);
        result = 31 * result + (hasAttachment ? 1 : 0);
        return result;
    }
}
