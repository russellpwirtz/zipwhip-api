package com.zipwhip.util;

import com.zipwhip.api.Address;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/29/11
 * Time: 12:59 PM
 */
public class AddressTest {

    @Test
    public void testParse() throws Exception {

        String parsableAddress = "scheme:scheme/authority/query";

        Address address = new Address();
        Assert.assertNull(address.parse(null));

        address.parse(parsableAddress);

        Assert.assertEquals(parsableAddress, address.getValue());
        Assert.assertEquals("schemescheme", address.getScheme());
        Assert.assertEquals("authority", address.getAuthority());
        Assert.assertEquals("query", address.getQuery());
    }

}
