package com.zipwhip.util;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/11/11
 * Time: 1:31 PM
 */
public class UrlUtilTest {

    String host = "http://network.zipwhip.com/";
    String apiVersion = "api/v1/";
    String method = "user/verify";
    String params = "?param1=hi&param2=mom";

    String apiKey = "123456";
    String apiSecret = "123456asdfasdf123456asdfasdf";

    SignTool authenticator;

    @Before
    public void setUp() throws Exception {
        authenticator = new SignTool(apiKey, apiSecret);
    }

    @Test
    public void testGetSignedUrlSessionKey() throws Exception {
        String sig = UrlUtil.getSignedUrl(host, apiVersion, method, params, "1443-2436546745637-636578-74487745:12345");
        Assert.assertNotNull(sig);
        System.out.println(sig);
    }

    @Test
    public void testGetSignedUrlAuthenticator() throws Exception {
        String sig = UrlUtil.getSignedUrl(host, apiVersion, method, params, authenticator);
        Assert.assertNotNull(sig);
        System.out.println(sig);
    }

    @Test
    public void testGetSignedUrl() throws Exception {
        String sig = UrlUtil.getSignedUrl(host, apiVersion, method, params, "1443-2436546745637-636578-74487745:12345", authenticator);
        Assert.assertNotNull(sig);
        System.out.println(sig);
    }

    @Test
    public void testSign() throws Exception {
        String sig = authenticator.sign("api/v1/user/verify?param1=hi&param2=mom");
        Assert.assertNotNull(sig);
        System.out.println(sig);
        Assert.assertEquals(sig, "0nLKjJhbFKt0rpm8lrLU1QYJ4dg=");
    }

//    @Test
//    public void testEncode() throws Exception {
//        String encoded = authenticator.encodeBase64("helloworld".getBytes());
//        Assert.assertNotNull(encoded);
//        System.out.println(encoded);
//        Assert.assertEquals(encoded, "aGVsbG93b3JsZA==");
//    }

}
