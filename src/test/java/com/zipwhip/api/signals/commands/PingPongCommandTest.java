package com.zipwhip.api.signals.commands;

import com.zipwhip.util.StringUtil;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/13/11
 * Time: 4:52 PM
 */
public class PingPongCommandTest {

    @Test
    public void testGetShortformInstance() throws Exception {

        PingPongCommand cmd = PingPongCommand.getShortformInstance();

        Assert.assertNotNull(cmd);
        Assert.assertFalse(cmd.isRequest());
        Assert.assertEquals(cmd.serialize(), StringUtil.EMPTY_STRING);
    }

    @Test
    public void testGetNewLongformInstance() throws Exception {
        PingPongCommand cmd = PingPongCommand.getNewLongformInstance();
        Assert.assertNotNull(cmd);

        Assert.assertFalse(cmd.isRequest());

        cmd.setRequest(true);
        Assert.assertTrue(cmd.isRequest());

        Assert.assertEquals(cmd.serialize(), "{\"action\":\"PONG\"}");

        cmd.setTimestamp(1234567890);
        Assert.assertEquals(cmd.serialize(), "{\"timestamp\":1234567890,\"action\":\"PONG\"}");

        cmd.setToken("hi");
        Assert.assertEquals(cmd.serialize(), "{\"timestamp\":1234567890,\"token\":\"hi\",\"action\":\"PONG\"}");
    }

}
