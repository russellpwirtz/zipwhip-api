package com.zipwhip.vendor;

import org.junit.Test;

import java.net.NoRouteToHostException;

import static org.junit.Assert.fail;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 5/31/12
 * Time: 11:33 AM
 */
public class AsyncVendorClientFactoryTest {

    public static String API_KEY = "jdo29chk";
    public static String API_SECRET = "anwcc99d-d152-ddw2-nmqp-oladwkn24dal90lot56s-9ns1-svm2-10b3-kd8bm21d9sl1";
    public static String HOST = "http://zipwhip.com";

    @Test
    public void testCreateViaApiKeyHost() throws Exception {
        AsyncVendorClient client = null;
        try {
            client = AsyncVendorClientFactory.createViaApiKey(API_KEY, API_SECRET, HOST);
            fail("Should have thrown a NoRouteToHostException. since hudson does not allow access to network.zipwhip.com");
        } catch (NoRouteToHostException e) {
            //This is the expected behavior until we fix the routing issue in super hudson
        }
//        Assert.assertEquals(HOST, client.getConnection().getHost());
    }

}
