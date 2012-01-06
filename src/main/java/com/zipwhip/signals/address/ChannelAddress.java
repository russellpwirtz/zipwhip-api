package com.zipwhip.signals.address;

import com.zipwhip.signals.message.MessageSerializer;
import com.zipwhip.signals.util.EncoderUtil;
import com.zipwhip.signals.util.SignalsFactory;
import com.zipwhip.signals.util.SignalsSerializer;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: Dec 11, 2010
 * Time: 7:53:59 PM
 * <p/>
 * To all consumers of a given channel.
 */
public class ChannelAddress extends AddressBase implements OneToManyAddress, SignalsFactory<ChannelAddress>, SignalsSerializer<ChannelAddress> {

	private static final long serialVersionUID = 1L;
	private static final String CHANNEL_KEY = "channel";

	private String channel;
	private String toString = null;

	public ChannelAddress() {
	}

	public ChannelAddress(String channel) {
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
		if (toString == null) {
			toString = StringUtil.join("{class:", getClass().getSimpleName(), ",channel:", channel, "}");
		}

		return toString;
	}

	@Override
	public ChannelAddress create(Map<String, Object> properties)
	{
		return new ChannelAddress(CollectionUtil.getString(properties, CHANNEL_KEY));
	}

	@Override
	public Map<String, Object> serialize(ChannelAddress item) {
		Map<String, Object> map = EncoderUtil.serialize(item);
		map.put(CHANNEL_KEY, item.channel);
		return map;
	}

	@Override
	public Map<String, Object> serialize(MessageSerializer serializer, ChannelAddress item) {
		return serialize(item);
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
		toString = null;
	}
}
