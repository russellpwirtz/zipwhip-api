package com.zipwhip.api.connection;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ApiConnectionConfiguration;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.util.Authenticator;
import com.zipwhip.util.StringUtil;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 11/14/12
 * Time: 11:43 AM
 *
 * This class moves a lot of the common stuff up a level so the underlying class can be
 * a lot cleaner.
 */
public abstract class ApiConnectionBase extends CascadingDestroyableBase implements ApiConnection {

    private Authenticator authenticator;
    private String version = DEFAULT_API_VERSION;
    private String host = ApiConnectionConfiguration.API_HOST;
    private String sessionKey;

    @Override
    public boolean isAuthenticated() {
        return StringUtil.exists(sessionKey) || authenticator != null;
    }

    @Override
    public boolean isConnected() {
        return isAuthenticated();
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getApiVersion() {
        return version;
    }

    public void setApiVersion(String version) {
        this.version = version;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }
}
