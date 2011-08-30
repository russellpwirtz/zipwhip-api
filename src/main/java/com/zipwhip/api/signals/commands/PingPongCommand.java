package com.zipwhip.api.signals.commands;

import com.zipwhip.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/30/11
 * Time: 1:10 PM
 */
public class PingPongCommand extends SerializingCommand {

    private static PingPongCommand instance;

    /**
     *  A private constructor to enforce use of the instance.
     */
    private PingPongCommand() {

    }

    public static PingPongCommand getInstance() {
        if (instance == null) {
            instance = new PingPongCommand();
        }

        return instance;
    }

    @Override
    public String serialize() {
        // CRLF is appended by StringToChannelBuffer, don't append here or we will be sending 2 PINGs
        return StringUtil.EMPTY_STRING;
    }

}
