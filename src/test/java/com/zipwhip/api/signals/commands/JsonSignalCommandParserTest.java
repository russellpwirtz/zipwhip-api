package com.zipwhip.api.signals.commands;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/29/11
 * Time: 5:47 PM
 */
public class JsonSignalCommandParserTest {

	public static final String CONNECT = "{\"action\":\"CONNECT\",\"clientId\":\"168d4470-c436-48d8-80bf-48bb9c1f8d7c\"}";
	public static final String DISCONNECT = "";
	public static final String SUB_COMPLETE = "{\"versionKey\":\"subscription_clientId_version_{class:ClientAddress,clientId:168d4470-c436-48d8-80bf-48bb9c1f8d7c}\",\"action\":\"SUBSCRIPTION_COMPLETE\",\"channels\":[{\"group\":\"1f70f915-9b1a-41ad-b30e-47b17cdb9f11:106228502\",\"channel\":\"/session/1f70f915-9b1a-41ad-b30e-47b17cdb9f11:106228502\"},{\"presence\":true,\"group\":\"1f70f915-9b1a-41ad-b30e-47b17cdb9f11:106228502\",\"channel\":\"/presence/5211ae17-d07f-465a-9cb4-0982d3c91952\"}],\"clientId\":\"168d4470-c436-48d8-80bf-48bb9c1f8d7c\",\"version\":1}";
	public static final String BACKLOG = "{\"action\":\"BACKLOG\",\"messages\":[{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/5211ae17-d07f-465a-9cb4-0982d3c91952}\",\"action\":\"SIGNAL\",\"signal\":{\"id\":\"14390512602\",\"content\":{\"to\":\"\",\"body\":\"Yo\",\"bodySize\":2,\"visible\":true,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"DELIVERED\"},\"type\":\"ZO\",\"metaDataId\":1080314702,\"dtoParentId\":106228502,\"scheduledDate\":null,\"thread\":\"\",\"carrier\":\"Tmo\",\"deviceId\":106228502,\"openMarketMessageId\":\"91b87b25-d444-48be-87af-110753917b57\",\"lastName\":\"\",\"class\":\"com.zipwhip.website.data.dto.Message\",\"isParent\":false,\"lastUpdated\":\"2011-08-30T10:17:38-07:00\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"deleted\":false,\"contactId\":268755902,\"uuid\":\"1c03922b-964b-47b7-a994-968a00c9c955\",\"isInFinalState\":true,\"statusDesc\":\"\",\"cc\":\"\",\"subject\":\"\",\"encoded\":true,\"expectDeliveryReceipt\":false,\"transferedToCarrierReceipt\":null,\"version\":3,\"statusCode\":0,\"id\":14390512602,\"fingerprint\":\"2216445311\",\"parentId\":0,\"phoneKey\":\"\",\"smartForwarded\":false,\"fromName\":\"\",\"isSelf\":false,\"firstName\":\"\",\"sourceAddress\":\"4252466003\",\"deliveryReceipt\":\"2011-08-30T10:17:38-07:00\",\"dishedToOpenMarket\":\"2011-08-30T10:17:33-07:00\",\"errorState\":false,\"creatorId\":213556702,\"advertisement\":\"\\n\\nSent via T-Mobile Messaging\",\"bcc\":\"\",\"fwd\":\"\",\"contactDeviceId\":106228502,\"smartForwardingCandidate\":false,\"destAddress\":\"2069308934\",\"DCSId\":\"\",\"latlong\":\"\",\"new\":false,\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-08-30T10:17:32-07:00\",\"UDH\":\"\",\"carbonedMessageId\":-1,\"mobileNumber\":\"2069308934\",\"channel\":\"\",\"isRead\":true},\"scope\":\"device\",\"reason\":null,\"tag\":null,\"event\":\"progress\",\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"message\",\"uri\":\"/signal/message/progress\"},\"channel\":\"/device/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"version\":25},{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/5211ae17-d07f-465a-9cb4-0982d3c91952}\",\"action\":\"SIGNAL\",\"signal\":{\"id\":\"14390586902\",\"content\":{\"to\":\"\",\"body\":\"Ok\",\"bodySize\":2,\"visible\":true,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"QUEUED\"},\"type\":\"MO\",\"metaDataId\":0,\"dtoParentId\":106228502,\"scheduledDate\":null,\"thread\":\"\",\"carrier\":\"Tmo\",\"deviceId\":106228502,\"openMarketMessageId\":\"\",\"lastName\":\"\",\"class\":\"com.zipwhip.website.data.dto.Message\",\"isParent\":false,\"lastUpdated\":\"2011-08-30T10:18:04-07:00\",\"loc\":\"\",\"messageConsoleLog\":\"Message created on Tue Aug 30 10:18:04 PDT 2011. Setting status code to 4 by default\",\"deleted\":false,\"contactId\":268755902,\"uuid\":\"6372b083-0662-4167-a172-9b58f6395c85\",\"isInFinalState\":false,\"statusDesc\":\"\",\"cc\":\"\",\"subject\":\"\",\"encoded\":true,\"expectDeliveryReceipt\":false,\"transferedToCarrierReceipt\":null,\"version\":1,\"statusCode\":4,\"id\":14390586902,\"fingerprint\":\"2216445311\",\"parentId\":0,\"phoneKey\":\"\",\"smartForwarded\":false,\"fromName\":null,\"isSelf\":false,\"firstName\":\"\",\"sourceAddress\":\"2069308934\",\"deliveryReceipt\":null,\"dishedToOpenMarket\":null,\"errorState\":false,\"creatorId\":0,\"advertisement\":null,\"bcc\":\"\",\"fwd\":\"\",\"contactDeviceId\":106228502,\"smartForwardingCandidate\":false,\"destAddress\":\"4252466003\",\"DCSId\":\"not parsed at the moment\",\"latlong\":\"\",\"new\":false,\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-08-30T10:18:00-07:00\",\"UDH\":\"\",\"carbonedMessageId\":-1,\"mobileNumber\":\"2069308934\",\"channel\":\"\",\"isRead\":false},\"scope\":\"device\",\"reason\":null,\"tag\":null,\"event\":\"receive\",\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"message\",\"uri\":\"/signal/message/receive\"},\"channel\":\"/device/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"version\":26},{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/5211ae17-d07f-465a-9cb4-0982d3c91952}\",\"action\":\"SIGNAL\",\"signal\":{\"id\":\"256757502\",\"content\":{\"lastContactFirstName\":\"\",\"lastContactLastName\":\"\",\"lastContactDeviceId\":0,\"unreadCount\":4,\"bcc\":\"\",\"lastUpdated\":\"2011-08-30T10:18:04-07:00\",\"class\":\"com.zipwhip.website.data.dto.Conversation\",\"deviceAddress\":\"device:/4252466003/0\",\"lastNonDeletedMessageDate\":\"2011-08-30T10:18:00-07:00\",\"deleted\":false,\"lastContactId\":268755902,\"lastMessageDate\":\"2011-08-30T10:18:00-07:00\",\"dtoParentId\":106228502,\"version\":42,\"lastContactMobileNumber\":\"2069308934\",\"id\":256757502,\"fingerprint\":\"2216445311\",\"new\":false,\"lastMessageBody\":\"Ok\",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-08-19T10:27:29-07:00\",\"cc\":\"\",\"deviceId\":106228502},\"scope\":\"device\",\"reason\":null,\"tag\":null,\"event\":\"change\",\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"conversation\",\"uri\":\"/signal/conversation/change\"},\"channel\":\"/device/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"version\":27},{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/5211ae17-d07f-465a-9cb4-0982d3c91952}\",\"action\":\"SIGNAL\",\"signal\":{\"id\":\"14390643402\",\"content\":{\"to\":\"\",\"body\":\"Ghb\",\"bodySize\":3,\"visible\":true,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"QUEUED\"},\"type\":\"MO\",\"metaDataId\":0,\"dtoParentId\":106228502,\"scheduledDate\":null,\"thread\":\"\",\"carrier\":\"Tmo\",\"deviceId\":106228502,\"openMarketMessageId\":\"\",\"lastName\":\"\",\"class\":\"com.zipwhip.website.data.dto.Message\",\"isParent\":false,\"lastUpdated\":\"2011-08-30T10:18:28-07:00\",\"loc\":\"\",\"messageConsoleLog\":\"Message created on Tue Aug 30 10:18:28 PDT 2011. Setting status code to 4 by default\",\"deleted\":false,\"contactId\":268755902,\"uuid\":\"f6c7fc21-c191-455f-a165-35097c7b717f\",\"isInFinalState\":false,\"statusDesc\":\"\",\"cc\":\"\",\"subject\":\"\",\"encoded\":true,\"expectDeliveryReceipt\":false,\"transferedToCarrierReceipt\":null,\"version\":1,\"statusCode\":4,\"id\":14390643402,\"fingerprint\":\"2216445311\",\"parentId\":0,\"phoneKey\":\"\",\"smartForwarded\":false,\"fromName\":null,\"isSelf\":false,\"firstName\":\"\",\"sourceAddress\":\"2069308934\",\"deliveryReceipt\":null,\"dishedToOpenMarket\":null,\"errorState\":false,\"creatorId\":0,\"advertisement\":null,\"bcc\":\"\",\"fwd\":\"\",\"contactDeviceId\":106228502,\"smartForwardingCandidate\":false,\"destAddress\":\"4252466003\",\"DCSId\":\"not parsed at the moment\",\"latlong\":\"\",\"new\":false,\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-08-30T10:18:23-07:00\",\"UDH\":\"\",\"carbonedMessageId\":-1,\"mobileNumber\":\"2069308934\",\"channel\":\"\",\"isRead\":false},\"scope\":\"device\",\"reason\":null,\"tag\":null,\"event\":\"receive\",\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"message\",\"uri\":\"/signal/message/receive\"},\"channel\":\"/device/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"version\":28},{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/5211ae17-d07f-465a-9cb4-0982d3c91952}\",\"action\":\"SIGNAL\",\"signal\":{\"id\":\"256757502\",\"content\":{\"lastContactFirstName\":\"\",\"lastContactLastName\":\"\",\"lastContactDeviceId\":0,\"unreadCount\":5,\"bcc\":\"\",\"lastUpdated\":\"2011-08-30T10:18:28-07:00\",\"class\":\"com.zipwhip.website.data.dto.Conversation\",\"deviceAddress\":\"device:/4252466003/0\",\"lastNonDeletedMessageDate\":\"2011-08-30T10:18:23-07:00\",\"deleted\":false,\"lastContactId\":268755902,\"lastMessageDate\":\"2011-08-30T10:18:23-07:00\",\"dtoParentId\":106228502,\"version\":43,\"lastContactMobileNumber\":\"2069308934\",\"id\":256757502,\"fingerprint\":\"2216445311\",\"new\":false,\"lastMessageBody\":\"Ghb\",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-08-19T10:27:29-07:00\",\"cc\":\"\",\"deviceId\":106228502},\"scope\":\"device\",\"reason\":null,\"tag\":null,\"event\":\"change\",\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"conversation\",\"uri\":\"/signal/conversation/change\"},\"channel\":\"/device/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"version\":29}]}";
	public static final String SIGNAL = "{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/5211ae17-d07f-465a-9cb4-0982d3c91952}\",\"action\":\"SIGNAL\",\"signal\":{\"content\":{\"to\":\"\",\"body\":\"Yo\",\"bodySize\":2,\"visible\":true,\"transmissionState\":{\"name\":\"QUEUED\",\"enumType\":\"com.zipwhip.outgoing.TransmissionState\"},\"type\":\"ZO\",\"metaDataId\":1040324202,\"dtoParentId\":106228502,\"scheduledDate\":null,\"thread\":\"\",\"carrier\":\"Tmo\",\"deviceId\":106228502,\"openMarketMessageId\":\"362c52b8-87ab-4e85-bbb5-f7a725ea0d7c\",\"lastName\":\"\",\"messageConsoleLog\":\"\",\"loc\":\"\",\"lastUpdated\":\"2011-08-25T12:02:41-07:00\",\"isParent\":false,\"class\":\"com.zipwhip.website.data.dto.Message\",\"deleted\":false,\"contactId\":268755902,\"isInFinalState\":false,\"uuid\":\"ce913542-93aa-421e-878a-5e9bad2b3ae6\",\"cc\":\"\",\"statusDesc\":\"\",\"subject\":\"\",\"encoded\":true,\"expectDeliveryReceipt\":false,\"transferedToCarrierReceipt\":null,\"version\":1,\"statusCode\":1,\"id\":13555722602,\"fingerprint\":\"2216445311\",\"parentId\":0,\"phoneKey\":\"\",\"smartForwarded\":false,\"fromName\":\"\",\"isSelf\":false,\"firstName\":\"\",\"sourceAddress\":\"4252466003\",\"deliveryReceipt\":null,\"dishedToOpenMarket\":null,\"errorState\":false,\"creatorId\":209644102,\"advertisement\":\"\\n\\nSent via T-Mobile Messaging\",\"bcc\":\"\",\"fwd\":\"\",\"contactDeviceId\":106228502,\"smartForwardingCandidate\":false,\"destAddress\":\"2069308934\",\"latlong\":\"\",\"DCSId\":\"\",\"new\":false,\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-08-25T12:02:41-07:00\",\"UDH\":\"\",\"carbonedMessageId\":-1,\"mobileNumber\":\"2069308934\",\"channel\":\"\",\"isRead\":true},\"id\":\"13555722602\",\"scope\":\"device\",\"reason\":null,\"event\":\"send\",\"tag\":null,\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"message\",\"uri\":\"/signal/message/send\"},\"channel\":\"/device/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"version\":6}";
	public static final String PRESENCE = "{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/presence/5211ae17-d07f-465a-9cb4-0982d3c91952}\",\"presence\":[{\"category\":\"NONE\",\"userAgent\":{\"product\":{\"name\":null,\"build\":null,\"version\":null},\"build\":null,\"makeModel\":null},\"address\":{\"clientId\":null},\"presenceStatus\":\"OFFLINE\",\"connected\":true,\"subscriptionId\":null,\"ip\":null}],\"action\":\"PRESENCE\",\"channel\":\"/presence/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"clientId\":\"67675cd3-7555-4849-99c2-33058a374ebd\",\"version\":517}";
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

	//@Test
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
	public void testParseBacklog() throws Exception {

		Command<?> cmd = parser.parse(BACKLOG);

		Assert.assertNotNull(cmd);
		Assert.assertTrue(cmd instanceof BacklogCommand);

		BacklogCommand backlogCommand = (BacklogCommand) cmd;

		Assert.assertNotNull(backlogCommand.getCommands());
	}

	@Test
	public void testParseSignal() throws Exception {

		Command<?> cmd = parser.parse(SIGNAL);

		Assert.assertNotNull(cmd);
		Assert.assertTrue(cmd instanceof SignalCommand);

		SignalCommand signalCommand = (SignalCommand) cmd;

		Assert.assertNotNull(signalCommand.getSignal());
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
