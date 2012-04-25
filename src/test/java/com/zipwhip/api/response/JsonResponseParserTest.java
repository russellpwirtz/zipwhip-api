package com.zipwhip.api.response;

import com.zipwhip.api.dto.EnrollmentResult;
import com.zipwhip.api.dto.MessageAttachment;
import com.zipwhip.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/14/11
 * Time: 1:37 PM
 */
public class JsonResponseParserTest {

    public JsonResponseParser parser;
    public ServerResponse response;

    public static final String ENROLLMENT_RESULT = "{\"response\":{\"carbonEnabled\":true,\"carbonInstalled\":true,\"deviceNumber\":999},\"sessions\":null,\"success\":true}";
    public static final String ENROLLMENT_RESULT_RESULT = "{\"carbonEnabled\":true,\"carbonInstalled\":true,\"deviceNumber\":999}";
    public final static String ATTACHMENT_RESULT = "{\"success\":true,\"response\":[{\"class\":\"com.zipwhip.website.data.dto.MessageAttachment\",\"dateCreated\":\"2012-04-24T15:42:25-07:00\",\"deviceId\":128918006,\"id\":160557306,\"lastUpdated\":null,\"messageId\":194919298488344576,\"messageType\":{\"enumType\":\"com.zipwhip.website.data.dto.MessageType\",\"name\":\"MO\"},\"new\":false,\"storageKey\":\"\",\"version\":0},{\"class\":\"com.zipwhip.website.data.dto.MessageAttachment\",\"dateCreated\":\"2012-04-24T15:42:25-07:00\",\"deviceId\":128918006,\"id\":160557406,\"lastUpdated\":null,\"messageId\":194919298488344576,\"messageType\":{\"enumType\":\"com.zipwhip.website.data.dto.MessageType\",\"name\":\"MO\"},\"new\":false,\"storageKey\":\"a011eacf-83a5-4b79-8999-81c0858591bd\",\"version\":0}]}";

    @Before
    public void setUp() throws Exception {
        parser = new JsonResponseParser();
    }

    @Test
    public void testParse() throws Exception {

        ServerResponse response = parser.parse(ENROLLMENT_RESULT);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccess());
        Assert.assertTrue(response instanceof ObjectServerResponse);
    }

    @Test
    public void testParseMessageTokens() throws Exception {

    }

    @Test
    public void testParseMessage() throws Exception {

    }

    @Test
    public void testParseString() throws Exception {

    }

    @Test
    public void testParseContact() throws Exception {

    }

    @Test
    public void testParseContacts() throws Exception {

    }

    @Test
    public void testParseDeviceToken() throws Exception {

    }

    @Test
    public void testParsePresence() throws Exception {

    }

    @Test
    public void testParseEnrollResult() throws Exception {

        JSONObject o = new JSONObject(ENROLLMENT_RESULT_RESULT);
        response = new ObjectServerResponse(ENROLLMENT_RESULT, true, o, null);


        EnrollmentResult result = parser.parseEnrollmentResult(response);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isCarbonEnabled());
        Assert.assertTrue(result.isCarbonInstalled());
        Assert.assertEquals(result.getDeviceNumber(), 999);
    }

    @Test
    public void testParseMessageAttachment() throws Exception {

        JSONArray o = new JSONObject(ATTACHMENT_RESULT).optJSONArray("response");
        response = new ArrayServerResponse(ENROLLMENT_RESULT, true, o, null);

        List<MessageAttachment> result = parser.parseAttachments(response);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());

        MessageAttachment dto1 = result.get(0);
        MessageAttachment dto2 = result.get(1);

        Assert.assertNotNull(dto1.getDateCreated());
        Assert.assertEquals(128918006L, dto1.getDeviceId());
        Assert.assertEquals(160557306L, dto1.getId());
        Assert.assertEquals(0L, dto1.getVersion());
        Assert.assertTrue(StringUtil.isNullOrEmpty(dto1.getStorageKey()));
        Assert.assertEquals(194919298488344576L, dto1.getMessageId());

        Assert.assertNotNull(dto2.getDateCreated());
        Assert.assertEquals(128918006L, dto2.getDeviceId());
        Assert.assertEquals(160557406L, dto2.getId());
        Assert.assertEquals(0L, dto2.getVersion());
        Assert.assertEquals("a011eacf-83a5-4b79-8999-81c0858591bd", dto2.getStorageKey());
        Assert.assertEquals(194919298488344576L, dto2.getMessageId());
    }

}
