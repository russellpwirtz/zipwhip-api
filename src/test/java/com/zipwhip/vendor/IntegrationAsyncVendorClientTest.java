package com.zipwhip.vendor;

import com.zipwhip.api.HttpConnection;
import com.zipwhip.concurrent.ObservableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: amoothart
 * Date: 12/5/11
 * Time: 10:44 AM
 * An end to end test using network connectivity
 */
public class IntegrationAsyncVendorClientTest {

    AsyncVendorClient client;

    String apiKey = "jdo29chk";
    String secret = "anwcc99d-d152-ddw2-nmqp-oladwkn24dal90lot56s-9ns1-svm2-10b3-kd8bm21d9sl1";
    String deviceAddress = "device:/2066810884/0";
    String contactMobileNumber = "4258946351";

    @Before
    public void setUp() throws Exception {
        client = AsyncVendorClientFactory.createViaApiKey(apiKey, secret);
        client.setConnection(new HttpConnection(apiKey, secret));
    }

    @Test
    public void testTextlineProvision() throws Exception {
        ObservableFuture<Void> result = client.textlineProvision("2066810884");
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void testTextlineEnroll() throws Exception {
        ObservableFuture<Void> result = client.textlineEnroll("2066810884", "amoothart@zipwhip.com");
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void testTextlineUnenroll() throws Exception {
        ObservableFuture<Void> result = client.textlineUnenroll("2066810884");
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
    }

}
