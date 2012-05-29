package com.zipwhip.api.response;

import com.zipwhip.api.dto.*;
import junit.framework.Assert;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/9/11
 * Time: 3:19 PM
 */
public class JsonDtoParserTest {

    JsonDtoParser parser;

    protected final static String CONTACT = "{\"id\":\"306322502\",\"content\":{\"birthday\":null,\"state\":\"\",\"version\":33,\"dtoParentId\":132961202,\"city\":\"\",\"id\":306322502,\"phoneKey\":\"\",\"isZwUser\":false,\"vector\":\"\",\"thread\":\"\",\"phoneId\":0,\"carrier\":\"Tmo\",\"firstName\":\"Ted\",\"deviceId\":132961202,\"lastName\":\"Hoffenator\",\"MOCount\":0,\"keywords\":\"\",\"zipcode\":\"\",\"ZOCount\":0,\"class\":\"com.zipwhip.website.data.dto.Contact\",\"lastUpdated\":\"2011-09-09T15:04:44-07:00\",\"loc\":\"\",\"targetGroupDevice\":-1,\"fwd\":\"\",\"deleted\":false,\"latlong\":\"\",\"new\":false,\"email\":\"\",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-09-08T15:00:21-07:00\",\"mobileNumber\":\"2069308999\",\"notes\":\"\",\"channel\":\"\"},\"scope\":\"device\",\"reason\":null,\"tag\":null,\"event\":\"change\",\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"type\":\"contact\",\"uri\":\"/signal/contact/change\"}";
    protected final static String MESSAGE = "{\"id\":\"15968846302\",\"content\":{\"to\":\"\",\"body\":\"Hello World\",\"bodySize\":11,\"visible\":true,\"transmissionState\":{\"enumType\":\"com.zipwhip.outgoing.TransmissionState\",\"name\":\"QUEUED\"},\"type\":\"ZO\",\"metaDataId\":1152387002,\"dtoParentId\":132961202,\"scheduledDate\":null,\"thread\":\"\",\"carrier\":\"Tmo\",\"deviceId\":132961202,\"openMarketMessageId\":\"de9b9868-a2d8-4736-a5ec-daa79579022b\",\"lastName\":\"\",\"class\":\"com.zipwhip.website.data.dto.Message\",\"isParent\":false,\"lastUpdated\":\"2011-09-09T15:05:10-07:00\",\"loc\":\"\",\"messageConsoleLog\":\"\",\"deleted\":true,\"contactId\":306322502,\"uuid\":\"86cd1738-ef9b-4695-ae5f-b4e93f7b5eb9\",\"isInFinalState\":false,\"statusDesc\":\"\",\"cc\":\"\",\"subject\":\"\",\"encoded\":true,\"expectDeliveryReceipt\":false,\"transferedToCarrierReceipt\":null,\"version\":3,\"statusCode\":1,\"id\":15968846302,\"fingerprint\":\"2216445311\",\"parentId\":0,\"phoneKey\":\"\",\"smartForwarded\":false,\"fromName\":\"\",\"isSelf\":false,\"firstName\":\"\",\"sourceAddress\":\"2063758020\",\"deliveryReceipt\":null,\"dishedToOpenMarket\":\"2011-09-08T15:21:46-07:00\",\"errorState\":false,\"creatorId\":222773802,\"advertisement\":\"Sent via T-Mobile Messaging\",\"bcc\":\"\",\"fwd\":\"\",\"contactDeviceId\":132961202,\"smartForwardingCandidate\":false,\"destAddress\":\"2069308934\",\"DCSId\":\"\",\"latlong\":\"\",\"new\":false,\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-09-08T15:21:46-07:00\",\"UDH\":\"\",\"carbonedMessageId\":-1,\"mobileNumber\":\"2069308934\",\"channel\":\"\",\"isRead\":true},\"scope\":\"device\",\"reason\":null,\"tag\":null,\"event\":\"delete\",\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"type\":\"message\",\"uri\":\"/signal/message/delete\"}";
    protected final static String CONVERSATION = "{\"id\":\"292476202\",\"content\":{\"lastContactFirstName\":\"Ted\",\"lastContactLastName\":\"Hoffenator\",\"lastContactDeviceId\":0,\"unreadCount\":0,\"bcc\":\"\",\"lastUpdated\":\"2011-09-09T15:04:44-07:00\",\"class\":\"com.zipwhip.website.data.dto.Conversation\",\"deviceAddress\":\"device:/2063758020/0\",\"lastNonDeletedMessageDate\":\"2011-09-09T15:04:44-07:00\",\"deleted\":false,\"lastContactId\":306322502,\"lastMessageDate\":\"2011-09-09T15:04:44-07:00\",\"dtoParentId\":132961202,\"version\":59,\"lastContactMobileNumber\":\"2069308934\",\"id\":292476202,\"fingerprint\":\"2216445311\",\"new\":false,\"lastMessageBody\":\"Hello World\",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-09-08T15:00:21-07:00\",\"cc\":\"\",\"deviceId\":132961202},\"scope\":\"device\",\"reason\":null,\"tag\":null,\"event\":\"change\",\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"type\":\"conversation\",\"uri\":\"/signal/conversation/change\"}";
    protected final static String DEVICE = "{\"id\":\"133533802\",\"content\":{\"cachedContactsCount\":0,\"class\":\"com.zipwhip.website.data.dto.Device\",\"lastUpdated\":\"2011-09-09T15:31:07-07:00\",\"type\":\"Group\",\"version\":1,\"textline\":\"\",\"dtoParentId\":129977302,\"linkedDeviceId\":132961202,\"id\":133533802,\"new\":false,\"phoneKey\":\"\",\"address\":\"device:/2063758020/5\",\"userId\":129977302,\"thread\":\"\",\"dateCreated\":\"2011-09-09T15:31:07-07:00\",\"uuid\":\"5e8cf187-5b51-4b7e-a462-04f88c896ff6\",\"displayName\":\"\",\"channel\":\"\",\"deviceId\":5},\"scope\":\"device\",\"reason\":null,\"tag\":null,\"event\":null,\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"2147bc3b-9ab4-4f35-98ea-80a3e4ca2d09\",\"type\":\"device\",\"uri\":\"/signal/device/null\"}";
    protected final static String CARBON = "{\"id\":null,\"content\":{\"carbonDescriptor\":\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\\r\\n<carbonEvents><carbonEvent><action>PROXY<\\/action><direction>OUTGOING<\\/direction><read>READ<\\/read><subject /><body>Hello World<\\/body><timestamp /><to>2069308934<\\/to><from>4252466003<\\/from><cc /><bcc /><userAgent /><handsetId>77b4f<\\/handsetId><sessionKey /><deviceCarbonVersion /><handsetInfo /><errorReason /><resetState /><transactionId /><resources /><contacts /><\\/carbonEvent><\\/carbonEvents>\\r\\n\",\"class\":\"com.zipwhip.incoming.carbon.OutgoingCarbonEvent\"},\"scope\":\"device\",\"reason\":null,\"tag\":null,\"event\":\"proxy\",\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"carbon\",\"uri\":\"/signal/carbon/proxy\"}";
    protected final static String ATTACHMENT = "{\"class\":\"com.zipwhip.website.data.dto.MessageAttachment\",\"dateCreated\":\"2012-04-24T15:42:25-07:00\",\"deviceId\":128918006,\"id\":160557406,\"lastUpdated\":\"2012-04-24T15:42:25-07:00\",\"messageId\":194919298488344576,\"messageType\":{\"enumType\":\"com.zipwhip.website.data.dto.MessageType\",\"name\":\"MO\"},\"new\":false,\"storageKey\":\"a011eacf-83a5-4b79-8999-81c0858591bd\",\"version\":0}";

    @Before
    public void setUp() throws Exception {
        parser = new JsonDtoParser();
    }

    @Test
    public void testParseContact() throws Exception {

        Contact dto = parser.parseContact(new JSONObject(CONTACT).optJSONObject("content"));
        Assert.assertNotNull(dto);

        Assert.assertEquals(dto.getAddress(), "ptn:/2069308934");
        Assert.assertEquals(dto.getCarrier(), "Tmo");
        Assert.assertEquals(dto.getChannel(), "");
        Assert.assertEquals(dto.getCity(), "");
        Assert.assertNotNull(dto.getDateCreated());
        Assert.assertEquals(dto.getDeviceId(), 132961202);
        Assert.assertEquals(dto.getEmail(), "");
        Assert.assertEquals(dto.getFirstName(), "Ted");
        Assert.assertEquals(dto.getFwd(), "");
        Assert.assertEquals(dto.getId(), 306322502);
        Assert.assertEquals(dto.getLastName(), "Hoffenator");
        Assert.assertNotNull(dto.getLastUpdated());
        Assert.assertEquals(dto.getLatlong(), "");
        Assert.assertEquals(dto.getMobileNumber(), "2069308999");
        Assert.assertEquals(dto.getMoCount(), 0);
        Assert.assertEquals(dto.getNotes(), "");
        Assert.assertEquals(dto.getPhoneKey(), "");
        Assert.assertEquals(dto.getState(), "");
        Assert.assertEquals(dto.getThread(), "");
        Assert.assertEquals(dto.getVersion(), 33);
        Assert.assertEquals(dto.getZipcode(), "");
        Assert.assertEquals(dto.getZoCount(), 0);
    }

    @Test
    public void testParseMessage() throws Exception {

        Message dto = parser.parseMessage(new JSONObject(MESSAGE).optJSONObject("content"));
        Assert.assertNotNull(dto);

        Assert.assertNotNull(dto.getTransmissionState());
        TransmissionState state = dto.getTransmissionState();
        Assert.assertEquals(state.toString(), "QUEUED");

        Assert.assertNotNull(dto.getDateCreated());
        Assert.assertNotNull(dto.getLastUpdated());
        Assert.assertEquals(dto.getMessageType(), "ZO");
        Assert.assertEquals(dto.getAddress(), "ptn:/2069308934");
        Assert.assertEquals(dto.getAdvertisement(), "Sent via T-Mobile Messaging");
        Assert.assertEquals(dto.getBcc(), "");
        Assert.assertEquals(dto.getBody(), "Hello World");
        Assert.assertEquals(dto.getCarrier(), "Tmo");
        Assert.assertEquals(dto.getCc(), "");
        Assert.assertEquals(dto.getChannel(), "");
        Assert.assertEquals(dto.getContactDeviceId(), 132961202);
        Assert.assertEquals(dto.getContactId(), 306322502);
        Assert.assertEquals(dto.getDestinationAddress(), "2069308934");
        Assert.assertEquals(dto.getDeviceId(), 132961202);
        Assert.assertEquals(dto.getFingerprint(), "2216445311");
        Assert.assertEquals(dto.getFirstName(), "");
        Assert.assertEquals(dto.getFwd(), "");
        Assert.assertEquals(dto.getLastName(), "");
        Assert.assertEquals(dto.getMessageType(), "ZO");
        Assert.assertEquals(dto.getMobileNumber(), "2069308934");
        Assert.assertEquals(dto.getSourceAddress(), "2063758020");
        Assert.assertEquals(dto.getStatusCode(), 1);
        Assert.assertEquals(dto.getStatusDesc(), "");
        Assert.assertEquals(dto.getSubject(), "");
        Assert.assertEquals(dto.getThread(), "");
        Assert.assertEquals(dto.getTo(), "");
        Assert.assertEquals(dto.getUuid(), "86cd1738-ef9b-4695-ae5f-b4e93f7b5eb9");
        Assert.assertEquals(dto.getVersion(), 3);
    }

    @Test
    public void testParseConversation() throws Exception {

        Conversation dto = parser.parseConversation(new JSONObject(CONVERSATION).optJSONObject("content"));
        Assert.assertNotNull(dto);

        Assert.assertNotNull(dto.getDateCreated());
        Assert.assertNotNull(dto.getLastMessageDate());
        Assert.assertNotNull(dto.getLastNonDeletedMessageDate());
        Assert.assertNotNull(dto.getLastUpdated());
        Assert.assertEquals(dto.getAddress(), "ptn:/2069308934");
        Assert.assertEquals(dto.getBcc(), "");
        Assert.assertEquals(dto.getCc(), "");
        Assert.assertEquals(dto.getDeviceAddress(), "device:/2063758020/0");
        Assert.assertEquals(dto.getDeviceId(), 132961202);
        Assert.assertEquals(dto.getFingerprint(), "2216445311");
        Assert.assertEquals(dto.getId(), 292476202);
        Assert.assertEquals(dto.getLastContactFirstName(), "Ted");
        Assert.assertEquals(dto.getLastContactLastName(), "Hoffenator");
        Assert.assertEquals(dto.getLastContactDeviceId(), 0);
        Assert.assertEquals(dto.getUnreadCount(), 0);
        Assert.assertEquals(dto.getLastContactId(), 306322502);
        Assert.assertEquals(dto.getLastContactMobileNumber(), "2069308934");
        Assert.assertEquals(dto.getLastMessageBody(), "Hello World");
        Assert.assertEquals(dto.getVersion(), 59);
    }

    @Test
    public void testParseMessageAttachment() throws Exception {

        MessageAttachment dto = parser.parseMessageAttachment(new JSONObject(ATTACHMENT));
        Assert.assertNotNull(dto);

        Assert.assertNotNull(dto.getDateCreated());
        Assert.assertNotNull(dto.getLastUpdated());
        Assert.assertEquals(128918006, dto.getDeviceId());
        Assert.assertEquals(160557406, dto.getId());
        Assert.assertEquals(0, dto.getVersion());
        Assert.assertEquals("a011eacf-83a5-4b79-8999-81c0858591bd", dto.getStorageKey());
        Assert.assertEquals(194919298488344576L, dto.getMessageId());
    }

    @Test
    public void testParseDevice() throws Exception {
        Device dto = parser.parseDevice(new JSONObject(DEVICE).optJSONObject("content"));
        Assert.assertNotNull(dto);

    }

    @Test
    public void testParseCarbonMessageContent() throws Exception {
        CarbonEvent dto = parser.parseCarbonEvent(new JSONObject(CARBON).optJSONObject("content"));
        Assert.assertNotNull(dto);
        Assert.assertEquals(dto.getCarbonDescriptor(), new JSONObject(CARBON).optJSONObject("content").optString("carbonDescriptor"));
    }

    @Test
    public void testParseLongVsString() throws Exception {

        String raw = "{\"id\":\"207533136530509825\",\"version\":0,\"dateCreated\":\"May 29, 2012 11:05:14 AM\",\"deviceId\":128918006,\"contactId\":220587806,\"creatorId\":318144706,\"metaDataId\":0,\"smartForwardingCandidate\":false,\"smartForwarded\":false,\"uuid\":\"207533136530509825\",\"errorState\":false,\"parentId\":0,\"isParent\":false,\"visible\":true,\"contactDeviceId\":128918006,\"openMarketMessageId\":\"\",\"isRead\":false,\"DCSId\":\"\",\"UDH\":\"\",\"messageConsoleLog\":\"\",\"body\":\"-1315812521\",\"bodySize\":99,\"type\":\"MO\",\"sourceAddress\":\"20000001\",\"destAddress\":\"2069308934\",\"expectDeliveryReceipt\":false,\"deleted\":false,\"statusCode\":4,\"carbonedMessageId\":-1,\"isInFinalState\":false,\"encoded\":false,\"thread\":\"\",\"channel\":\"\",\"fwd\":\"\",\"mobileNumber\":\"20000001\",\"address\":\"ptn:/20000001\",\"phoneKey\":\"\",\"firstName\":\"\",\"lastName\":\"\",\"carrier\":\"Unknown\",\"isSelf\":false,\"latlong\":\"\",\"loc\":\"\",\"subject\":\"\",\"fingerprint\":\"3705583301\",\"hasAttachment\":false,\"transmissionState\":\"DELIVERED\",\"contact\":{\"id\":220587806,\"version\":110,\"lastUpdated\":\"May 25, 2012 12:06:48 PM\",\"dateCreated\":\"Mar 27, 2012 3:08:59 PM\",\"deviceId\":128918006,\"loc\":\"\",\"latlong\":\"\",\"firstName\":\"\",\"lastName\":\"\",\"email\":\"\",\"mobileNumber\":\"20000001\",\"address\":\"ptn:/20000001\",\"phoneId\":0,\"phoneKey\":\"\",\"city\":\"\",\"state\":\"\",\"zipcode\":\"\",\"isZwUser\":false,\"deleted\":false,\"carrier\":\"Unknown\",\"targetGroupDevice\":-1,\"keywords\":\"\",\"thread\":\"\",\"channel\":\"\",\"fwd\":\"\",\"vector\":\"\",\"ZOCount\":0,\"MOCount\":0,\"notes\":\"\"}},\"event\":\"receive\",\"scope\":\"device\",\"uri\":\"/signal/message/receive\",\"type\":\"message\",\"uuid\":\"d081cac9-ff61-404d-9d42-f9c2e74f9c32\"}";
        JSONObject response = new JSONObject(raw);

        // The old code did this...
        Assert.assertNotSame(207533136530509825L, response.optLong("id"));

        Assert.assertEquals(207533136530509825L, Long.parseLong("207533136530509825"));
        Assert.assertEquals(207533136530509825L, Long.parseLong(response.optString("id")));
        Assert.assertEquals("207533136530509825", response.optString("uuid"));

    }

}
