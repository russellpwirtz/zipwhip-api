package com.zipwhip.api.response;

import com.zipwhip.api.dto.EnrollmentResult;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

}
