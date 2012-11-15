package com.zipwhip.api;

import com.zipwhip.api.connection.Connection;
import com.zipwhip.util.Authenticator;

/**
 * A {@code Connection} encapsulates the way to communicate to Zipwhip API on behalf of user.
 * <p/>
 * A connection needs to be authenticated as a specific user.
 *
 * @author Jed
 */
public interface ApiConnection extends Connection {

    /**
     * The default Zipwhip domain
     */
    public static final String DEFAULT_HOST = "http://network.zipwhip.com";

    /**
     * The default Zipwhip HTTPS domain
     */
    public static final String DEFAULT_HTTPS_HOST = "https://network.zipwhip.com";

    /**
     * The Zipwhip staging domain
     */
    public static final String STAGING_HOST = "http://staging.zipwhip.com";

    /**
     * The Zipwhip test domain
     */
    public static final String TEST_HOST = "http://test.zipwhip.com";

    /**
     * Signal Server staging host
     */
    public static final String DEFAULT_SIGNALS_HOST = "74.209.177.242";

    /**
     * Signal Server test host
     */
    public static final String TEST_SIGNALS_HOST = "74.209.177.241";

    /**
     * Signal Server staging host
     */
    public static final String STAGING_SIGNALS_HOST = "69.46.44.181";

    /**
     * Signal Server port 80
     */
    public static final int DEFAULT_SIGNALS_PORT = 80;

    /**
     * Signal Server port 443
     */
    public static final int PORT_443 = 443;

    /**
     * Signal Server port 3000
     */
    public static final int PORT_3000 = 3000;

    /**
     * Signal Server port 8080
     */
    public static final int PORT_8080 = 8080;

    /**
     * The default Zipwhip API version
     */
    public static final String DEFAULT_API_VERSION = "/";

    /**
     * Set the {@code SignTool} to be used for authentication.
     *
     * @param authenticator The {@code SignTool} to be used for authentication.
     */
    void setAuthenticator(Authenticator authenticator);

    /**
     * Get the {@code SignTool} to be used for authentication.
     *
     * @return The {@code SignTool} to be used for authentication.
     */
    Authenticator getAuthenticator();

    /**
     * Set the host to connect to. The default is {@code DEFAULT_HOST}.
     *
     * @param host The host to connect to.
     */
    public void setHost(String host);

    /**
     * Get the host to connect to. The default is {@code DEFAULT_HOST}.
     * @return The host to connect to. The default is {@code DEFAULT_HOST}.
     */
    public String getHost();

    /**
     * Get the Zipwhip API version to be used in web calls.
     * @param apiVersion The Zipwhip API version to be used in web calls.
     */
    void setApiVersion(String apiVersion);

    /**
     * Set the Zipwhip API version to be used in web calls.
     * @return The Zipwhip API version to be used in web calls.
     */
    String getApiVersion();

    /**
     * Set the sessionKey for this connection
     *
     * @param sessionKey the client's Zipwhip sessionKey
     */
    void setSessionKey(String sessionKey);

    /**
     * Get this connection's sessionKey
     *
     * @return sessionKey
     */
    String getSessionKey();

}
