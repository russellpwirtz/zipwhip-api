package com.zipwhip.api;

import com.zipwhip.util.SignTool;

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
     * The default Zipwhip API version
     */
    public static final String DEFAULT_API_VERSION = "/";

    /**
     * Set the {@code SignTool} to be used for authentication.
     *
     * @param authenticator The {@code SignTool} to be used for authentication.
     */
    void setAuthenticator(SignTool authenticator);

    /**
     * Get the {@code SignTool} to be used for authentication.
     *
     * @return The {@code SignTool} to be used for authentication.
     */
    SignTool getAuthenticator();

    /**
     * Set the host to connect to. The default is {@code DEFAULT_HOST}.
     *
     * @param host The host to connect to.
     */
    void setHost(String host);

    /**
     * Get the host to connect to. The default is {@code DEFAULT_HOST}.
     * @return The host to connect to. The default is {@code DEFAULT_HOST}.
     */
    String getHost();

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

}
