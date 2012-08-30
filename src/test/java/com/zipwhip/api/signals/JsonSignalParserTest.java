package com.zipwhip.api.signals;

import com.zipwhip.api.dto.Contact;
import com.zipwhip.api.dto.Conversation;
import com.zipwhip.api.dto.Device;
import com.zipwhip.api.dto.Message;
import junit.framework.Assert;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/9/11
 * Time: 5:28 PM
 */
public class JsonSignalParserTest {

	JsonSignalParser parser;

	protected final static String CONTACT = "{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09}\",\"action\":\"SIGNAL\",\"signal\":{\"content\":{\"birthday\":null,\"state\":\"\",\"city\":\"\",\"dtoParentId\":132961202,\"version\":35,\"id\":306322502,\"phoneKey\":\"\",\"isZwUser\":false,\"vector\":\"\",\"thread\":\"\",\"carrier\":\"Tmo\",\"phoneId\":0,\"firstName\":\"Jed\",\"deviceId\":132961202,\"lastName\":\"Hoffenator\",\"MOCount\":0,\"keywords\":\"\",\"zipcode\":\"\",\"ZOCount\":0,\"class\":\"com.zipwhip.website.data.dto.Contact\",\"lastUpdated\":\"2011-09-12T10:21:04-07:00\",\"loc\":\"\",\"targetGroupDevice\":-1,\"fwd\":\"\",\"deleted\":false,\"latlong\":\"\",\"new\":false,\"address\":\"ptn:/2069308934\",\"email\":\"\",\"dateCreated\":\"2011-09-08T15:00:21-07:00\",\"notes\":\"\",\"mobileNumber\":\"2069308934\",\"channel\":\"\"},\"id\":\"306322502\",\"scope\":\"device\",\"reason\":null,\"event\":\"change\",\"tag\":null,\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"type\":\"contact\",\"uri\":\"/signal/contact/change\"},\"channel\":\"/device/2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"version\":1}";
	protected final static String MESSAGE = "{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09}\",\"action\":\"SIGNAL\",\"signal\":{\"content\":{\"to\":\"\",\"body\":\"Hello World\",\"bodySize\":11,\"visible\":true,\"transmissionState\":{\"name\":\"QUEUED\",\"enumType\":\"com.zipwhip.outgoing.TransmissionState\"},\"type\":\"ZO\",\"metaDataId\":1174158602,\"dtoParentId\":132961202,\"scheduledDate\":null,\"thread\":\"\",\"carrier\":\"Tmo\",\"deviceId\":132961202,\"openMarketMessageId\":\"ec5c1d09-9caa-4b1a-b207-4df0b04411bf\",\"lastName\":\"Hoffenator\",\"messageConsoleLog\":\"\",\"loc\":\"\",\"lastUpdated\":\"2011-09-12T10:21:05-07:00\",\"isParent\":false,\"class\":\"com.zipwhip.website.data.dto.Message\",\"deleted\":false,\"contactId\":306322502,\"isInFinalState\":false,\"uuid\":\"74fe7498-9b3f-4f62-a867-e3f4f87175fd\",\"cc\":\"\",\"statusDesc\":\"\",\"subject\":\"\",\"encoded\":true,\"expectDeliveryReceipt\":false,\"transferedToCarrierReceipt\":null,\"version\":2,\"statusCode\":1,\"id\":16698424302,\"fingerprint\":\"2216445311\",\"parentId\":0,\"phoneKey\":\"\",\"smartForwarded\":false,\"fromName\":\"\",\"isSelf\":false,\"firstName\":\"Jed\",\"sourceAddress\":\"2063758020\",\"deliveryReceipt\":null,\"dishedToOpenMarket\":\"2011-09-12T10:21:05-07:00\",\"errorState\":false,\"creatorId\":228673902,\"advertisement\":\"\",\"bcc\":\"\",\"fwd\":\"\",\"contactDeviceId\":132961202,\"smartForwardingCandidate\":false,\"destAddress\":\"2069308934\",\"latlong\":\"\",\"DCSId\":\"\",\"new\":false,\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-09-12T10:21:04-07:00\",\"UDH\":\"\",\"carbonedMessageId\":-1,\"mobileNumber\":\"2069308934\",\"channel\":\"\",\"isRead\":true},\"id\":\"16698424302\",\"scope\":\"device\",\"reason\":null,\"event\":\"send\",\"tag\":null,\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"type\":\"message\",\"uri\":\"/signal/message/send\"},\"channel\":\"/device/2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"version\":2}";
	protected final static String CONVERSATION = "{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09}\",\"action\":\"SIGNAL\",\"signal\":{\"content\":{\"lastContactFirstName\":\"Jed\",\"lastContactLastName\":\"Hoffenator\",\"lastContactDeviceId\":0,\"unreadCount\":0,\"bcc\":\"\",\"lastUpdated\":\"2011-09-12T10:21:05-07:00\",\"class\":\"com.zipwhip.website.data.dto.Conversation\",\"deviceAddress\":\"device:/2063758020/0\",\"lastNonDeletedMessageDate\":\"2011-09-12T10:21:04-07:00\",\"deleted\":false,\"lastContactId\":306322502,\"dtoParentId\":132961202,\"version\":62,\"lastMessageDate\":\"2011-09-12T10:21:04-07:00\",\"id\":292476202,\"lastContactMobileNumber\":\"2069308934\",\"fingerprint\":\"2216445311\",\"new\":false,\"lastMessageBody\":\"Hello World\",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-09-08T15:00:21-07:00\",\"deviceId\":132961202,\"cc\":\"\"},\"id\":\"292476202\",\"scope\":\"device\",\"reason\":null,\"event\":\"change\",\"tag\":null,\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"type\":\"conversation\",\"uri\":\"/signal/conversation/change\"},\"channel\":\"/device/2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"version\":5}";
	protected final static String DEVICE = "{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09}\",\"action\":\"SIGNAL\",\"signal\":{\"content\":{\"cachedContactsCount\":0,\"class\":\"com.zipwhip.website.data.dto.Device\",\"lastUpdated\":\"2011-09-12T10:38:24-07:00\",\"type\":\"Group\",\"version\":1,\"textline\":\"\",\"dtoParentId\":129977302,\"linkedDeviceId\":132961202,\"id\":134690802,\"new\":false,\"phoneKey\":\"\",\"address\":\"device:/2063758020/7\",\"thread\":\"\",\"userId\":129977302,\"dateCreated\":\"2011-09-12T10:38:24-07:00\",\"uuid\":\"4d2dc7f5-de5e-4116-a8ca-0a27922c7c79\",\"displayName\":\"\",\"channel\":\"\",\"deviceId\":7},\"id\":\"134690802\",\"scope\":\"device\",\"reason\":null,\"event\":null,\"tag\":null,\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"type\":\"device\",\"uri\":\"/signal/device/null\"},\"channel\":\"/device/2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"version\":20}";

	@Before
	public void setUp() throws Exception {
		parser = new JsonSignalParser();
	}

	@Test
	public void testParseContactSignal() throws Exception {
		Signal s = parser.parseSignal(new JSONObject(CONTACT));
		Assert.assertNotNull(s);
		Assert.assertNotNull(s.content);
		Assert.assertTrue(s.content instanceof Contact);

	}

	@Test
	public void testParseMessageSignal() throws Exception {
		Signal s = parser.parseSignal(new JSONObject(MESSAGE));
		Assert.assertNotNull(s);
		Assert.assertNotNull(s.content);
		Assert.assertTrue(s.content instanceof Message);

	}

	@Test
	public void testParseConversationSignal() throws Exception {
		Signal s = parser.parseSignal(new JSONObject(CONVERSATION));
		Assert.assertNotNull(s);
		Assert.assertNotNull(s.content);
		Assert.assertTrue(s.content instanceof Conversation);

	}

	@Test
	public void testParseDeviceSignal() throws Exception {
		Signal s = parser.parseSignal(new JSONObject(DEVICE));
		Assert.assertNotNull(s);
		Assert.assertNotNull(s.content);
		Assert.assertTrue(s.content instanceof Device);

	}

	@Test
	public void testParseSignalFromSignalServer() throws Exception {
		String json = "{\"content\":{\"cachedContactsCount\":0,\"type\":\"Website\",\"version\":0,\"textline\":\"\",\"linkedDeviceId\":0,\"id\":0,\"phoneKey\":\"\",\"address\":\"\",\"userId\":0,\"thread\":\"\",\"dateCreated\":\"Dec 23, 2011 2:33:20 PM\",\"uuid\":\"2363ff64-58ad-46f5-9dcc-eb19b99dd6be\",\"displayName\":\"Website\",\"channel\":\"\",\"deviceId\":0},\"scope\":\"device\",\"reason\":\"something happened\",\"uuid\":\"0c2c7dc1-4082-4d10-a926-d43321b4bde5\",\"type\":\"device\"}";
		Signal s = parser.parseSignal(new JSONObject(json));
		assertNotNull(s);
		assertNotNull(s.getContent());
		assertTrue(s.getContent() instanceof Device);
	}

	@Test
	public void testParseSignalFromSignalServerDate() throws Exception {
		String json = "{\"content\":{\"cachedContactsCount\":0,\"type\":\"Website\",\"version\":0,\"textline\":\"\",\"linkedDeviceId\":0,\"id\":0,\"phoneKey\":\"\",\"address\":\"\",\"userId\":0,\"thread\":\"\",\"dateCreated\":\"1970-01-01T00:00:00Z\",\"uuid\":\"2363ff64-58ad-46f5-9dcc-eb19b99dd6be\",\"displayName\":\"Website\",\"channel\":\"\",\"deviceId\":0},\"scope\":\"device\",\"reason\":\"something happened\",\"uuid\":\"0c2c7dc1-4082-4d10-a926-d43321b4bde5\",\"type\":\"device\"}";

		Signal s = parser.parseSignal(new JSONObject(json));
		assertNotNull(s);
		assertNotNull(s.getContent());
		assertTrue(s.getContent() instanceof Device);
		assertEquals("Wed Dec 31 16:00:00 PST 1969", ((Device) s.getContent()).getDateCreated().toString());
	}
}
