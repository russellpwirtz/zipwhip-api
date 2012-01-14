package com.zipwhip.api.signals.commands;

import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 1/13/12
 * Time: 4:23 PM
 */
public class BackfillCommandTest {
    
    static final String CHANNEL = "channel:/1234-5678-1234";

    @Test
    public void testSerialize() throws Exception {

        List<Long> commands = new ArrayList<Long>();
        commands.add(1l);

        BackfillCommand backfill = new BackfillCommand(commands, CHANNEL);
        String serializedString = backfill.serialize();
        Assert.assertEquals("{\"commands\":[1],\"action\":\"BACKFILL\",\"channel\":\"channel:/1234-5678-1234\"}", serializedString);
        
        commands.add(12l);
        backfill = new BackfillCommand(commands, CHANNEL);
        serializedString = backfill.serialize();
        Assert.assertEquals("{\"commands\":[1,12],\"action\":\"BACKFILL\",\"channel\":\"channel:/1234-5678-1234\"}", serializedString);

    }

}
