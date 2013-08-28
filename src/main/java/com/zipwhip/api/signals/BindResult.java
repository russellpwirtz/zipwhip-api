package com.zipwhip.api.signals;

import java.io.Serializable;

/**
 * Date: 8/27/13
 * Time: 3:38 PM
 *
 * @author Michael
 * @version 1
 */
public class BindResult implements Serializable {

    private static final long serialVersionUID = -41223725604503270L;

    private final String clientId;
    private final String token;

    public BindResult(String clientId, String token) {
        this.clientId = clientId;
        this.token = token;
    }

    public String getClientId() {
        return clientId;
    }

    public String getToken() {
        return token;
    }
}
