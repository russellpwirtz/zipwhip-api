package com.zipwhip.api.dto;

/**
 * Created by IntelliJ IDEA.
 * * Date: Jul 17, 2009
 * Time: 8:23:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageToken {

    /**
     * This is the message uuid
     */
    String message;
    long deviceId;
    long contactId;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

}
