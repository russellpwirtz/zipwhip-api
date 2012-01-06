package com.zipwhip.signals.address;

import com.zipwhip.signals.message.MessageSerializer;
import com.zipwhip.signals.util.EncoderUtil;
import com.zipwhip.signals.util.SignalsFactory;
import com.zipwhip.signals.util.SignalsSerializer;
import com.zipwhip.util.CollectionUtil;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: David Davis
 * Date: 8/26/11
 * Time: 1:55 PM
 *
 */
public abstract class ClientAddressBase extends AddressBase implements OneToOneAddress, SignalsFactory<ClientAddress>, SignalsSerializer<ClientAddress> {

	private static final long serialVersionUID = 1L;
	private static final String KEY_CLIENT_ID = "clientId";

	@Override
	public ClientAddress create(Map<String, Object> properties)
	{
		return new ClientAddress(CollectionUtil.getString(properties, KEY_CLIENT_ID));
	}

	@Override
	public Map<String, Object> serialize(ClientAddress item) {
		Map<String, Object> map = EncoderUtil.serialize(item);

		map.put(KEY_CLIENT_ID, item.getClientId());

		return map;
	}

	@Override
	public Map<String, Object> serialize(MessageSerializer serializer, ClientAddress item) {
		return serialize(item);
	}
}
