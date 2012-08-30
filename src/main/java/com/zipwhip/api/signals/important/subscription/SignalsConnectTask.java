package com.zipwhip.api.signals.important.subscription;

import com.zipwhip.important.ImportantTask;
import com.zipwhip.important.tasks.SimpleImportantTask;
import com.zipwhip.util.DateUtil;

import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 4:47 PM
 */
public class SignalsConnectTask extends SimpleImportantTask<SignalsConnectParameters> implements ImportantTask<SignalsConnectParameters> {

    public SignalsConnectTask(String sessionKey, String clientId, long timeoutInSeconds) {
        super(
            // this determines the worker that's run
            "/signals/connect",
            // these are the params when the worker runs
            new SignalsConnectParameters(sessionKey, clientId),
            // this is the expiration date (null to never expire)
            DateUtil.inFuture(timeoutInSeconds, TimeUnit.SECONDS),
            // this prevents concurrent dupes
            "/signals/connect"+sessionKey+clientId);
    }

    public SignalsConnectTask(String sessionKey, String clientId) {
        this(sessionKey, clientId, 30);
    }

}
