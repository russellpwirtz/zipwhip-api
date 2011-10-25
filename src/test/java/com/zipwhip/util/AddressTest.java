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
    public void testStringToMobileNumber() throws Exception {

        String deviceAddress = "device:/5555555555/0";
        String ptnAddress = "ptn:/5555555555";

        Assert.assertEquals("5555555555", Address.stripToMobileNumber(deviceAddress));
        Assert.assertEquals("5555555555", Address.stripToMobileNumber(ptnAddress));
    }

    @Test
    public void testParse() throws Exception {

        String parsableAddress = "device:/5555555555/0";

        Address address = new Address(null);
        Assert.assertNull(address.getScheme());
        Assert.assertNull(address.getAuthority());

        address = new Address(parsableAddress);

        Assert.assertEquals(parsableAddress, address.getValue());
        Assert.assertEquals("device", address.getScheme());
        Assert.assertEquals("5555555555", address.getAuthority());
        Assert.assertEquals("0", address.getQuery());
    }

    @Test
    public void testEncode() throws Exception {

        String nonParsableAddress = "5555555555";

        Address address = new Address(nonParsableAddress);

        Assert.assertEquals(nonParsableAddress, address.getValue());
        Assert.assertNull(address.getScheme());
        Assert.assertNull(address.getAuthority());
        Assert.assertNull(address.getQuery());

        Assert.assertEquals(Address.encode("device", nonParsableAddress, "0"), "device:/5555555555/0");
        Assert.assertEquals(Address.encode("ptn", nonParsableAddress), "ptn:/5555555555");
    }

}
