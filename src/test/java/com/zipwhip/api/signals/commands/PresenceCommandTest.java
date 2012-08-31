package com.zipwhip.api.signals.commands;

import com.zipwhip.api.signals.PresenceUtil;
import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.message.Action;
import com.zipwhip.signals.presence.*;
import junit.framework.Assert;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/29/11
 * Time: 5:36 PM
 */
public class PresenceCommandTest {

	public static final String IP = "10.1.1.255";
	public static final String SUB_ID = "subscription__123456-123456";
	public static final String CLIENT_ID = "1234-1234-1234-1234";
	public static final String P_BUILD = "Zipwhip";
	public static final String VERSION = "1.0.1";
	public static final String U_BUILD = "2.3.4";
	public static final String MODEL = "HTC Wow";

	PresenceCommand command;

	Presence presence;
	ClientAddress address;
	UserAgent userAgent;
	Product product;

	@Before
	public void setUp() throws Exception {

		presence = new Presence();
		presence.setConnected(Boolean.TRUE);
		presence.setIp(IP);
		presence.setSubscriptionId(SUB_ID);
		presence.setCategory(PresenceCategory.Phone);
		presence.setStatus(PresenceStatus.ONLINE);

		address = new ClientAddress();
		address.setClientId(CLIENT_ID);
		presence.setAddress(address);

		product = new Product();
		product.setBuild(P_BUILD);
		product.setVersion(VERSION);
		product.setName(ProductLine.ZIPGROUPS);

		userAgent = new UserAgent();
		userAgent.setProduct(product);
		userAgent.setBuild(U_BUILD);
		userAgent.setMakeModel(MODEL);
		presence.setUserAgent(userAgent);

		command = new PresenceCommand(Collections.singletonList(presence));
	}

	@Test
	public void testSerialize() throws Exception {

		String jsonString = command.serialize();

		JSONObject presenceObject = new JSONObject(jsonString);
		Assert.assertNotNull(presenceObject);

		JSONArray presenceArray = presenceObject.optJSONArray(Action.PRESENCE.name());
		Assert.assertNotNull(presenceArray);

		List<Presence> presences = PresenceUtil.getInstance().parse(presenceArray);

		for (Presence p : presences) {
			Assert.assertEquals(p.getAddress().getClientId(), CLIENT_ID);
			Assert.assertEquals(p.getCategory(), PresenceCategory.Phone);
			Assert.assertTrue(p.getConnected());
			Assert.assertEquals(p.getIp(), IP);
			Assert.assertEquals(p.getStatus(), PresenceStatus.ONLINE);
			Assert.assertEquals(p.getSubscriptionId(), SUB_ID);
			Assert.assertEquals(p.getUserAgent().getBuild(), U_BUILD);
			Assert.assertEquals(p.getUserAgent().getMakeModel(), MODEL);
			Assert.assertEquals(p.getUserAgent().getProduct().getBuild(), P_BUILD);
			Assert.assertEquals(p.getUserAgent().getProduct().getName(), ProductLine.ZIPGROUPS);
			Assert.assertEquals(p.getUserAgent().getProduct().getVersion(), VERSION);
		}
	}

}
