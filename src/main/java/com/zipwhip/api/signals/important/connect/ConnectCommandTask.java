package com.zipwhip.api.signals.important.connect;

import com.zipwhip.util.DateUtil;
import com.zipwhip.important.tasks.SimpleImportantTask;

import java.util.Date;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 12:58 PM
 */
public class ConnectCommandTask extends SimpleImportantTask<ConnectCommandParameters> {

    public ConnectCommandTask(String clientId) {
        this(clientId, new Date(System.currentTimeMillis() + 30000));
    }

    public ConnectCommandTask(String clientId, Date expirationDate) {
        super("connect", new ConnectCommandParameters(clientId), expirationDate, "connect");
    }

    public ConnectCommandTask(String clientId, Date expirationDate, Map<String, Long> versions) {
        super("connect", new ConnectCommandParameters(clientId, null, versions), expirationDate, "connect");
    }

    public ConnectCommandTask(String clientId, Map<String, Long> versions) {
        this(clientId, DateUtil.in30Seconds(), versions);
    }
}
