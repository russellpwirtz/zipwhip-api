package com.zipwhip.signals.address;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: Dec 11, 2010
 * Time: 7:53:59 PM
 * <p/>
 * To all consumers of a given channel.
 */
public class ChannelAddress extends AddressBase implements Serializable {

    private static final long serialVersionUID = 6712566321988288131L;

	private String channel;
	private String string = null;

	public ChannelAddress() {

	}

	public ChannelAddress(String channel) {
        this();

		this.channel = channel;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}

		ChannelAddress that = (ChannelAddress) o;

		if (channel != null ? !channel.equals(that.channel) : that.channel != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return channel != null ? channel.hashCode() : 0;
	}

	@Override
	public String toString() {
		if (string == null) {
            string = "{\"channel\":\"" + string + channel + "\"}";
		}

		return string;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
        this.string = null;
		this.channel = channel;
	}
}
