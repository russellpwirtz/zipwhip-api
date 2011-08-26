package com.zipwhip.api;

import com.zipwhip.lifecycle.Destroyable;

import java.util.Map;
import java.util.concurrent.Future;

/**
 * A connection encapsulates the way to communicate to Zipwhip on behalf of a
 * user.
 * 
 * A connection needs to be authenticated as a specific user.
 * 
 * @author Michael
 */
public interface Connection extends Destroyable {

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

    /**
     * Determines if this connection is authenticated with Zipwhip. (it has the
     * necessary communication fields)
     * 
     * @return true if authenticated otherwise false
     */
    boolean isAuthenticated();

    /**
     * Determines if the connection is available/stable.
     * 
     * @return true if connected otherwise false.
     */
    boolean isConnected();

    /**
     * Transmit a packet to Zipwhip. Will be executed in a background thread.
     * 
     * @param method
     *        each method has a name. example: user/get
     * @param params Map of query params to append to the method
     * @return A Future task representing the asynchronous call to Zipwhip
     * @throws Exception is an error is encountered communicating with Zipwhip or parsing a response
     */
    Future<String> send(String method, Map<String, Object> params) throws Exception;

}
