package com.zipwhip.signals.message;

import com.zipwhip.signals.address.Address;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: Dec 11, 2010
 * Time: 4:32:58 PM
 * 
 * A basic class that is sent between actors. Every message must have a command and an address that
 * dictates who the message is destined for.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 9130714327015373270L;

    private Address address;
    private long timestamp;
    private String id;
    private String type;
    private String event;
    private Serializable content;

    /**
	 * Where is this message going to!
	 *
	 * @return
	 */
	public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Serializable getContent() {
        return content;
    }

    public void setContent(Serializable content) {
        this.content = content;
    }
}
