package com.zipwhip.api.connection;

import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.lifecycle.Destroyable;

import java.io.InputStream;

/**
 * A connection encapsulates the way to communicate on behalf of user.
 * <p/>
 * A connection needs to be authenticated as a specific user.
 *
 * The purpose of this interface is to allow us to easily transition to sending requests over the Signal Server at
 * a later time. Currently we are only using HTTP for requests, though the vision is to transition to a pure
 * SignalServer socket reuse strategy. Talk is also being made of allowing SMS based transports.
 *
 * @author Michael
 */
public interface Connection extends Destroyable {

    String getSessionKey();

    /**
     * Determines if this connection is authenticated with Zipwhip, if it has the
     * necessary communication fields.
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
     * Execute a call to the Zipwhip API ASYNCHRONOUSLY.
     *
     * @param method Each method has a name, example: user/get. See {@link com.zipwhip.api.ZipwhipNetworkSupport} for fields.
     * @param uri The path to the resource (ie: message/list)
     * @param body The payload of the request
     * @return A ObservableFuture task which will return the response body on completion.
     * @throws Exception is an error is encountered communicating with Zipwhip or parsing a response
     */
    ObservableFuture<InputStream> send(RequestMethod method, String uri, RequestBody body) throws Exception;

}
