package com.zipwhip.vendor;

import com.zipwhip.api.ApiConnection;
import junit.framework.Assert;
import org.junit.Test;

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
        AsyncVendorClient client = AsyncVendorClientFactory.createViaApiKey(API_KEY, API_SECRET, HOST);
        Assert.assertEquals(HOST, ((ApiConnection)client.getConnection()).getHost());
    }

}
