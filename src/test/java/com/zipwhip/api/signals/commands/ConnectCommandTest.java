package com.zipwhip.api.signals.commands;

import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/29/11
 * Time: 2:41 PM
 */
public class ConnectCommandTest {

    ConnectCommand command;
    String serial;

    @Test
    public void testIsSuccessful() throws Exception {

        command = new ConnectCommand(null);
        Assert.assertFalse(command.isSuccessful());

        command = new ConnectCommand("123456");
        Assert.assertTrue(command.isSuccessful());
    }

    @Test
    public void testSerializeAction() throws Exception {

        command = new ConnectCommand(null);
        serial = command.serialize();
        Assert.assertEquals("{\"action\":\"CONNECT\"}", serial);

        command = new ConnectCommand("123456");
        serial = command.serialize();
        Assert.assertEquals("{\"action\":\"CONNECT\",\"clientId\":\"123456\"}", serial);
    }

    @Test
    public void testSerializeClientId() throws Exception {

        command = new ConnectCommand("123456");
        serial = command.serialize();
        Assert.assertEquals("{\"action\":\"CONNECT\",\"clientId\":\"123456\"}", serial);
    }

    @Test
    public void testSerializeVersions() throws Exception {

        Map<String, Long> versions = new HashMap<String, Long>();
        versions.put("ver1",1L);
        versions.put("ver2",2L);
        command = new ConnectCommand("123456", versions);
        serial = command.serialize();
        Assert.assertEquals("{\"versions\":{\"ver2\":2,\"ver1\":1},\"action\":\"CONNECT\",\"clientId\":\"123456\"}", serial);
    }

}
