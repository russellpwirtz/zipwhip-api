package com.zipwhip.api.response;

import com.zipwhip.api.dto.SignalToken;
import com.zipwhip.api.signals.JsonSignal;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/12/11
 * Time: 6:02 PM
 */
public class JsonSignalTokenParserTest {

    public JsonSignalTokenParser parser;

    public static final String SIGNAL_TOKEN = "{\"subscriptionEntry\":{\"index\":null,\"encodedSubscriptionSettings\":\"\",\"class\":\"com.zipwhip.website.data.dto.SubscriptionEntry\",\"lastUpdated\":\"2011-10-14T00:00:00-07:00\",\"subscriptionId\":245,\"version\":1,\"dtoParentId\":270315,\"id\":4824,\"subscriptionKey\":\"aol-push\",\"new\":false,\"signalFilters\":\"/signal\",\"active\":true,\"dateCreated\":\"2011-10-14T00:00:00-07:00\",\"deviceId\":270315},\"mobileNumber\":\"2063758020\",\"signals\":[{\"id\":\"408050\",\"content\":{\"birthday\":null,\"state\":\"\",\"version\":3,\"dtoParentId\":270315,\"city\":\"\",\"id\":408050,\"phoneKey\":\"\",\"isZwUser\":false,\"vector\":\"\",\"thread\":\"20000002\",\"phoneId\":0,\"carrier\":\"Tmo\",\"firstName\":\"\",\"deviceId\":270315,\"lastName\":\"\",\"MOCount\":0,\"keywords\":\"\",\"zipcode\":\"\",\"ZOCount\":0,\"class\":\"com.zipwhip.website.data.dto.Contact\",\"lastUpdated\":\"2011-10-14T14:59:05-07:00\",\"loc\":\"\",\"targetGroupDevice\":-1,\"fwd\":\"20000102\",\"deleted\":false,\"latlong\":\"\",\"new\":false,\"email\":\"\",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-10-14T14:58:54-07:00\",\"mobileNumber\":\"2069308934\",\"notes\":\"\",\"channel\":\"2\"},\"scope\":\"device\",\"reason\":null,\"tag\":null,\"event\":\"change\",\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"6cc57ab9-1fdc-4f0d-8a93-2a11da868b3c\",\"type\":\"contact\",\"uri\":\"/signal/contact/change\"}]}";

    @Before
    public void setUp() throws Exception {
        parser = new JsonSignalTokenParser();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testParse() throws Exception {

        SignalToken token = parser.parse(SIGNAL_TOKEN);

        Assert.assertNotNull(token);
        Assert.assertEquals(token.getMobileNumber(), "2063758020");

        Assert.assertNotNull(token.getSubscriptionEntry());
        Assert.assertEquals(token.getSubscriptionEntry().getSignalFilters(), "/signal");
        Assert.assertEquals(token.getSubscriptionEntry().getSubscriptionKey(), "aol-push");

        Assert.assertTrue(token.getSignals().size() == 1);
        Assert.assertTrue(token.getSignals().get(0) instanceof JsonSignal);
    }

}
