package com.zipwhip.api.signals;

import com.zipwhip.api.dto.SignalToken;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/12/11
 * Time: 3:40 PM
 *
 * Processes signalTokens that come into our system
 */
public interface SignalTokenProcessor {

    void process(SignalToken signalToken);

}
