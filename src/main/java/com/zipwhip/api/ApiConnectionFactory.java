package com.zipwhip.api;

import com.zipwhip.api.response.JsonResponseParser;
import com.zipwhip.api.response.ResponseParser;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.response.StringServerResponse;
import com.zipwhip.concurrent.ConfiguredFactory;
import com.zipwhip.concurrent.ExecutorFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.util.SignTool;
import com.zipwhip.util.Factory;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Creates a Connection with the specified parameters.
 */
public class ApiConnectionFactory implements Factory<ApiConnection> {

    private static final Logger LOGGER = Logger.getLogger(ApiConnectionFactory.class);

    private ResponseParser responseParser = new JsonResponseParser();

    private ConfiguredFactory<String, ExecutorService> bossExecutorFactory;
    private ConfiguredFactory<String, ExecutorService> workerExecutorFactory;

    private String host = ApiConnectionConfiguration.API_HOST;
    private String apiVersion = ApiConnection.DEFAULT_API_VERSION;
    private String username;
    private String password;
    private String apiKey;
    private String secret;
    private String sessionKey;

    private Factory<ApiConnection> factory;

    protected ApiConnectionFactory(Factory<ApiConnection> factory) {
        this.factory = factory;
    }

    public static ApiConnectionFactory newInstance() {
        return new ApiConnectionFactory();
    }

    public static ApiConnectionFactory newAsyncInstance() {
        return new ApiConnectionFactory() {
            @Override
            protected ApiConnection createInstance(Executor bossExecutor, Executor workerExecutor) {
                return new NingHttpConnection(bossExecutor, workerExecutor);
            }
        };
//        return new ApiConnectionFactory(NingHttpConnection.class);
    }

    public static ApiConnectionFactory newAsyncHttpsInstance() {
        ApiConnectionFactory connectionFactory = newAsyncInstance();

        connectionFactory.setHost(ApiConnection.DEFAULT_HTTPS_HOST);

        return connectionFactory;
    }

    /**
     * Creates a generic unauthenticated ApiConnection.
     *
     * @return Connection an authenticated Connection
     */
    @Override
    public ApiConnection create() {

        try {
            ApiConnection connection = createInstance();

            connection.setSessionKey(sessionKey);
            connection.setApiVersion(apiVersion);
            connection.setHost(host);

            if (StringUtil.exists(apiKey) && StringUtil.exists(secret)) {
                connection.setAuthenticator(new SignTool(apiKey, secret));
            }

            // We have a username/password
            if (StringUtil.exists(username) && StringUtil.exists(password)) {

                Map<String, Object> params = new HashMap<String, Object>();
                params.put("mobileNumber", username);
                params.put("password", password);

                ObservableFuture<String> future = connection.send("user/login", params);

                future.awaitUninterruptibly();

                if (!future.isSuccess()) {
                    throw new RuntimeException("Cannot create connection, login rejected");
                }

                ServerResponse serverResponse = responseParser.parse(future.getResult());

                if (!serverResponse.isSuccess()) {
                    throw new RuntimeException("Error authenticating client");
                }

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

    protected ApiConnection createInstance() {

    }

    protected ApiConnection createInstance(Executor bossExecutor, Executor workerExecutor) {
        return new HttpConnection(bossExecutor, workerExecutor);
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

    public ApiConnectionFactory apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public ResponseParser getResponseParser() {
        return responseParser;
    }

    public void setResponseParser(ResponseParser responseParser) {
        this.responseParser = responseParser;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public ApiConnection getConnection() {
        return connection;
    }

    public void setConnection(ApiConnection connection) {
        this.connection = connection;
    }

    public static class NingConnectionFactory implements Factory<ApiConnection> {
        return
    }

    public static class HttpConnectionFactory implements Factory<ApiConnection> {

        private final ConfiguredFactory<String, ExecutorService> bossExecutorFactory;
        private final ConfiguredFactory<String, ExecutorService> workerExecutorFactory;

        public HttpConnectionFactory(ConfiguredFactory<String, ExecutorService> workerExecutorFactory, ConfiguredFactory<String, ExecutorService> bossExecutorFactory) {
            this.workerExecutorFactory = workerExecutorFactory;
            this.bossExecutorFactory = bossExecutorFactory;
        }

        @Override
        public ApiConnection create() {
            Executor bossExecutor = null;
            if (bossExecutorFactory != null) {
                bossExecutor = bossExecutorFactory.create("ApiConnection-boss");
            }
            Executor workerExecutor = null;
            if (workerExecutorFactory != null) {
                workerExecutor = workerExecutorFactory.create("ApiConnection-worker");
            }

            return new HttpConnection(bossExecutor,  workerExecutor);
        }
    }

}
