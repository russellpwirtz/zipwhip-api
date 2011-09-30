package com.zipwhip.api;

import com.zipwhip.api.response.JsonResponseParser;
import com.zipwhip.api.response.ResponseParser;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.response.StringServerResponse;
import com.zipwhip.util.SignTool;
import com.zipwhip.util.Factory;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Creates a Connection with the specified parameters.
 */
public class ApiConnectionFactory implements Factory<ApiConnection> {

    private static final Logger LOGGER = Logger.getLogger(ApiConnectionFactory.class);

    private ResponseParser responseParser = new JsonResponseParser();

    private String host = ApiConnection.DEFAULT_HOST;
    private String username;
    private String password;
    private String apiKey;
    private String secret;
    private String sessionKey;

    public static ApiConnectionFactory newInstance() {
        return new ApiConnectionFactory();
    }

    /**
     * Creates a generic unauthenticated ApiConnection.
     *
     * @return Connection an authenticated Connection
     */
    @Override
    public ApiConnection create() {

        try {
            ApiConnection connection = new NingHttpConnection();

            connection.setSessionKey(sessionKey);
            connection.setHost(host);
            connection.setAuthenticator(new SignTool(apiKey, secret));

            // The authenticator should be ready go to.
            if (StringUtil.exists(apiKey) && StringUtil.exists(secret)) {

                if (StringUtil.isNullOrEmpty(sessionKey)) {
                    // we need a sessionKey
                    requestSessionKey(connection);
                }
            }

            // We have a username/password
            else if (StringUtil.exists(username) && StringUtil.exists(password)) {

                Map<String, Object> params = new HashMap<String, Object>();
                params.put("mobileNumber", username);
                params.put("password", password);

                Future<String> future = connection.send("user/login", params);
                ServerResponse serverResponse = responseParser.parse(future.get());

                //DeviceToken token = responseParser.parseDeviceToken(serverResponse);
                //connection.setAuthenticator(new SignTool(token.getApiKey(), token.getSecret()));
                //connection.setSessionKey(token.getSessionKey());

                if (serverResponse instanceof StringServerResponse) {
                    connection.setSessionKey(((StringServerResponse) serverResponse).response);
                }
            }

            return connection;

        } catch (Exception e) {

            LOGGER.error("Error creating Connection", e);

            return null;
        }
    }

    protected void requestSessionKey(final Connection connection) throws Exception {

        Map<String, Object> params = new HashMap<String, Object>();

        params.put("apiKey", apiKey);

        Future<String> future = connection.send(ZipwhipNetworkSupport.SESSION_GET, params);

        String sessionKey = responseParser.parseString(responseParser.parse(future.get()));

        if (StringUtil.isNullOrEmpty(sessionKey)) {
            throw new Exception("Retrieving a sessionKey failed");
        }

        connection.setSessionKey(sessionKey);
    }

    public ApiConnectionFactory responseParser(ResponseParser responseParser) {
        this.responseParser = responseParser;
        return this;
    }

    public ApiConnectionFactory username(String username) {
        this.username = username;
        return this;
    }

    public ApiConnectionFactory password(String password) {
        this.password = password;
        return this;
    }

    public ApiConnectionFactory apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public ApiConnectionFactory secret(String secret) {
        this.secret = secret;
        return this;
    }

    public ApiConnectionFactory sessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
        return this;
    }

    public ApiConnectionFactory host(String host) {
        this.host = host;
        return this;
    }

}
