package com.zipwhip.api.signals;

import com.zipwhip.api.dto.SignalToken;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/12/11
 * Time: 3:44 PM
 *
 * A simple implementation to help with debugging
 */
public class LoggingSignalTokenProcessor implements SignalTokenProcessor {

    private static final Logger LOGGER = Logger.getLogger(LoggingSignalTokenProcessor.class);

    @Override
    public void process(SignalToken signalToken) {

        if (signalToken == null){
            LOGGER.warn("Got null signalToken");
            return;
        }

        LOGGER.debug("Got token " + signalToken.toString());
    }

}
