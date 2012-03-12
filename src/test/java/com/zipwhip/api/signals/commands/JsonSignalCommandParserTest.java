package com.zipwhip.api.signals.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA. User: jed Date: 8/29/11 Time: 5:47 PM
 */
public class JsonSignalCommandParserTest {

	public static final String CONNECT = "{\"action\":\"CONNECT\",\"clientId\":\"168d4470-c436-48d8-80bf-48bb9c1f8d7c\"}";
	public static final String DISCONNECT = "";
	public static final String SUB_COMPLETE = "{\"versionKey\":\"subscription_clientId_version_{class:ClientAddress,clientId:168d4470-c436-48d8-80bf-48bb9c1f8d7c}\",\"action\":\"SUBSCRIPTION_COMPLETE\",\"channels\":[{\"group\":\"1f70f915-9b1a-41ad-b30e-47b17cdb9f11:106228502\",\"channel\":\"/session/1f70f915-9b1a-41ad-b30e-47b17cdb9f11:106228502\"},{\"presence\":true,\"group\":\"1f70f915-9b1a-41ad-b30e-47b17cdb9f11:106228502\",\"channel\":\"/presence/5211ae17-d07f-465a-9cb4-0982d3c91952\"}],\"clientId\":\"168d4470-c436-48d8-80bf-48bb9c1f8d7c\",\"version\":1}";
	public static final String SIGNAL = "{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/5211ae17-d07f-465a-9cb4-0982d3c91952}\",\"action\":\"SIGNAL\",\"signal\":{\"content\":{\"to\":\"\",\"body\":\"Yo\",\"bodySize\":2,\"visible\":true,\"transmissionState\":{\"name\":\"QUEUED\",\"enumType\":\"com.zipwhip.outgoing.TransmissionState\"},\"type\":\"ZO\",\"metaDataId\":1040324202,\"dtoParentId\":106228502,\"scheduledDate\":null,\"thread\":\"\",\"carrier\":\"Tmo\",\"deviceId\":106228502,\"openMarketMessageId\":\"362c52b8-87ab-4e85-bbb5-f7a725ea0d7c\",\"lastName\":\"\",\"messageConsoleLog\":\"\",\"loc\":\"\",\"lastUpdated\":\"2011-08-25T12:02:41-07:00\",\"isParent\":false,\"class\":\"com.zipwhip.website.data.dto.Message\",\"deleted\":false,\"contactId\":268755902,\"isInFinalState\":false,\"uuid\":\"ce913542-93aa-421e-878a-5e9bad2b3ae6\",\"cc\":\"\",\"statusDesc\":\"\",\"subject\":\"\",\"encoded\":true,\"expectDeliveryReceipt\":false,\"transferedToCarrierReceipt\":null,\"version\":1,\"statusCode\":1,\"id\":13555722602,\"fingerprint\":\"2216445311\",\"parentId\":0,\"phoneKey\":\"\",\"smartForwarded\":false,\"fromName\":\"\",\"isSelf\":false,\"firstName\":\"\",\"sourceAddress\":\"4252466003\",\"deliveryReceipt\":null,\"dishedToOpenMarket\":null,\"errorState\":false,\"creatorId\":209644102,\"advertisement\":\"\\n\\nSent via T-Mobile Messaging\",\"bcc\":\"\",\"fwd\":\"\",\"contactDeviceId\":106228502,\"smartForwardingCandidate\":false,\"destAddress\":\"2069308934\",\"latlong\":\"\",\"DCSId\":\"\",\"new\":false,\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-08-25T12:02:41-07:00\",\"UDH\":\"\",\"carbonedMessageId\":-1,\"mobileNumber\":\"2069308934\",\"channel\":\"\",\"isRead\":true},\"id\":\"13555722602\",\"scope\":\"device\",\"reason\":null,\"event\":\"send\",\"tag\":null,\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"message\",\"uri\":\"/signal/message/send\"},\"channel\":\"/device/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"version\":6}";
	public static final String PRESENCE = "{\"PRESENCE\":[{\"address\":{\"clientId\":\"164102886693933056\",\"toString\":\"{class:ClientAddress,clientId:164102886693933056}\"},\"category\":\"Desktop\",\"userAgent\":{\"build\":\"test\",\"product\":{\"name\":\"DESKTOP_APP\",\"version\":\"1.0.01\",\"build\":\"test\"}},\"status\":\"ONLINE\",\"connected\":true,\"subscriptionId\":\"/presence/10c0f34c-9372-44b7-bd45-804f5439f277\",\"lastActive\":\"Jan 30, 2012 2:13:45 PM\"}],\"action\":\"PRESENCE\",\"class\":\"com.zipwhip.api.signals.commands.PresenceCommand\"}";
	public static final String PING_PONG = "{\"action\":\"PING\",\"request\":true,\"timestamp\":1234567890}";
	public static final String VERIFY = "{\"action\":\"SIGNAL_VERIFICATION\"}";
	public static final String NOOP = "{\"action\":\"NOOP\"}";

	JsonSignalCommandParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new JsonSignalCommandParser();
	}

	@Test
	public void testParseConnect() throws Exception {

		Command<?> cmd = parser.parse(CONNECT);

		Assert.assertNotNull(cmd);
		Assert.assertTrue(cmd instanceof ConnectCommand);

		ConnectCommand connectCommand = (ConnectCommand) cmd;

		Assert.assertEquals(connectCommand.getClientId(), "168d4470-c436-48d8-80bf-48bb9c1f8d7c");
	}

	// @Test
	public void testParseDisconnect() throws Exception {

		Command<?> cmd = parser.parse(DISCONNECT);

		Assert.assertNotNull(cmd);
		Assert.assertTrue(cmd instanceof DisconnectCommand);

		DisconnectCommand disconnectCommand = (DisconnectCommand) cmd;

		Assert.assertEquals(disconnectCommand.getHost(), "");
		Assert.assertEquals(disconnectCommand.getPort(), 0);
		Assert.assertEquals(disconnectCommand.getReconnectDelay(), 0);
	}

	@Test
	public void testParseSubscriptionComplete() throws Exception {

		Command<?> cmd = parser.parse(SUB_COMPLETE);

		Assert.assertNotNull(cmd);
		Assert.assertTrue(cmd instanceof SubscriptionCompleteCommand);

		SubscriptionCompleteCommand subscriptionCompleteCommand = (SubscriptionCompleteCommand) cmd;

		Assert.assertNotNull(subscriptionCompleteCommand.getCommands());
	}

	@Test
	public void testParseSignal() throws Exception {

		Command<?> cmd = parser.parse(SIGNAL);

		assertNotNull(cmd);
		assertTrue(cmd instanceof SignalCommand);

		SignalCommand signalCommand = (SignalCommand) cmd;

		assertNotNull(signalCommand.getSignal());
		assertFalse(signalCommand.isBackfill());
		assertEquals(Long.valueOf(SignalCommand.NOT_BACKFILL_SIGNAL_VERSION), Long.valueOf(signalCommand.getMaxBackfillVersion()));
	}

	@Test
	public void testParseSignalWithBackfill() throws Exception {
		String signal = "{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/5211ae17-d07f-465a-9cb4-0982d3c91952}\","
				+ "\"action\":\"SIGNAL\",\"isBackfill\":true,\"maxBackfillVersion\":123456,\"signal\":{\"content\":{\"to\":\"\",\"body\":\"Yo\",\"bodySize\":2,\"visible\":true,"
				+ "\"transmissionState\":{\"name\":\"QUEUED\",\"enumType\":\"com.zipwhip.outgoing.TransmissionState\"},\"type\":\"ZO\","
				+ "\"metaDataId\":1040324202,\"dtoParentId\":106228502,\"scheduledDate\":null,\"thread\":\"\",\"carrier\":\"Tmo\","
				+ "\"deviceId\":106228502,\"openMarketMessageId\":\"362c52b8-87ab-4e85-bbb5-f7a725ea0d7c\",\"lastName\":\"\","
				+ "\"messageConsoleLog\":\"\",\"loc\":\"\",\"lastUpdated\":\"2011-08-25T12:02:41-07:00\",\"isParent\":false,"
				+ "\"class\":\"com.zipwhip.website.data.dto.Message\",\"deleted\":false,\"contactId\":268755902,"
				+ "\"isInFinalState\":false,\"uuid\":\"ce913542-93aa-421e-878a-5e9bad2b3ae6\",\"cc\":\"\",\"statusDesc\":\"\","
				+ "\"subject\":\"\",\"encoded\":true,\"expectDeliveryReceipt\":false,\"transferedToCarrierReceipt\":null,\"version\":1,"
				+ "\"statusCode\":1,\"id\":13555722602,\"fingerprint\":\"2216445311\",\"parentId\":0,\"phoneKey\":\"\","
				+ "\"smartForwarded\":false,\"fromName\":\"\",\"isSelf\":false,\"firstName\":\"\",\"sourceAddress\":\"4252466003\","
				+ "\"deliveryReceipt\":null,\"dishedToOpenMarket\":null,\"errorState\":false,\"creatorId\":209644102,"
				+ "\"advertisement\":\"\\n\\nSent via T-Mobile Messaging\",\"bcc\":\"\",\"fwd\":\"\",\"contactDeviceId\":106228502,"
				+ "\"smartForwardingCandidate\":false,\"destAddress\":\"2069308934\",\"latlong\":\"\",\"DCSId\":\"\",\"new\":false,"
				+ "\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-08-25T12:02:41-07:00\",\"UDH\":\"\",\"carbonedMessageId\":-1,"
				+ "\"mobileNumber\":\"2069308934\",\"channel\":\"\",\"isRead\":true},\"id\":\"13555722602\",\"scope\":\"device\","
				+ "\"reason\":null,\"event\":\"send\",\"tag\":null,\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":"
				+ "\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"message\",\"uri\":\"/signal/message/send\"},"
				+ "\"channel\":\"/device/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"version\":6}";

		Command<?> cmd = parser.parse(signal);

		assertNotNull(cmd);
		assertTrue(cmd instanceof SignalCommand);

		SignalCommand signalCommand = (SignalCommand) cmd;

		assertNotNull(signalCommand.getSignal());
		assertTrue(signalCommand.isBackfill());
		assertEquals(Long.valueOf(123456), Long.valueOf(signalCommand.getMaxBackfillVersion()));
	}

	@Test
	public void testParsePresence() throws Exception {

		Command<?> cmd = parser.parse(PRESENCE);

		Assert.assertTrue(cmd instanceof PresenceCommand);

		PresenceCommand presenceCommand = (PresenceCommand) cmd;

		Assert.assertNotNull(presenceCommand.getPresence());
	}

	@Test
	public void testParseVerification() throws Exception {

		Command<?> cmd = parser.parse(VERIFY);

		Assert.assertNotNull(cmd);
		Assert.assertTrue(cmd instanceof SignalVerificationCommand);
	}

	@Test
	public void testParseNoop() throws Exception {

		Command<?> cmd = parser.parse(NOOP);

		Assert.assertNotNull(cmd);
		Assert.assertTrue(cmd instanceof NoopCommand);
	}

	@Test
	public void testParsePingPongCommand() throws Exception {

		Command<?> cmd = parser.parse(PING_PONG);

		Assert.assertNotNull(cmd);
		Assert.assertTrue(cmd instanceof PingPongCommand);

		PingPongCommand pingPongCommand = (PingPongCommand) cmd;
		Assert.assertEquals(pingPongCommand.getTimestamp(), 1234567890);
		Assert.assertTrue(pingPongCommand.isRequest());
	}

}
