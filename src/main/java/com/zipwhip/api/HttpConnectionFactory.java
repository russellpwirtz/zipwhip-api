package com.zipwhip.api;

import com.zipwhip.api.response.JsonResponseParser;
import com.zipwhip.api.response.ResponseParser;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.response.StringServerResponse;
import com.zipwhip.lib.SignTool;
import com.zipwhip.util.Factory;
import com.zipwhip.util.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: 7/7/11
 * Time: 2:12 PM
 * <p/>
 * Creates HttpConnection.
 */
public class HttpConnectionFactory implements Factory<Connection> {

    private ResponseParser responseParser = new JsonResponseParser();

    private String host = HttpConnection.DEFAULT_HOST;
    private String username;
    private String password;
    private String apiKey;
    private String secret;
    private String sessionKey;

    private static HttpConnectionFactory instance;

    public static HttpConnectionFactory getInstance() {
        if (instance == null) {
            instance = new HttpConnectionFactory();
        }
        return instance;
    }

    /**
     * Creates a generic unauthenticated HttpConnection.
     *
     * @return
     * @throws Exception
     */
    @Override
    public Connection create() throws Exception {

        HttpConnection connection = new HttpConnection();

        connection.setSessionKey(sessionKey);
        connection.setHost(host);
        connection.setAuthenticator(new SignTool(apiKey, secret));

        if (StringUtil.exists(apiKey) && StringUtil.exists(secret)) {
            // good, the authenticator should be ready go to.
            if (StringUtil.isNullOrEmpty(sessionKey)) {
                // we need a sessionKey
                requestSessionKey(connection);
            }
        } else if (StringUtil.exists(username) && StringUtil.exists(password)) {
            // we have a username/password

            Map<String, Object> params = new HashMap<String, Object>();

            params.put("mobileNumber", username);
            params.put("password", password);

            Future<String> future = connection.send("login", params);
            ServerResponse serverResponse = responseParser.parse(future.get());
//            DeviceToken token = responseParser.parseDeviceToken(serverResponse);

//            connection.setAuthenticator(new SignTool(token.apiKey, token.secret));
//            connection.setSessionKey(token.sessionKey);

            if (serverResponse instanceof StringServerResponse) {
                connection.setSessionKey(((StringServerResponse) serverResponse).response);
            }
        }

        return connection;
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

    public HttpConnectionFactory setResponseParser(ResponseParser responseParser) {
        this.responseParser = responseParser;
        return this;
    }

    public HttpConnectionFactory setUsername(String username) {
        this.username = username;
        return this;
    }

    public HttpConnectionFactory setPassword(String password) {
        this.password = password;
        return this;
    }

    public HttpConnectionFactory setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public HttpConnectionFactory setSecret(String secret) {
        this.secret = secret;
        return this;
    }

    public HttpConnectionFactory setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
        return this;
    }

    public HttpConnectionFactory setHost(String host) {
        this.host = host;
        return this;
    }

}
