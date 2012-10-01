package com.zipwhip.api;

/**
 * Get a configured, or default if not configured, configuration for connecting to Zipwhip web and signals APIs.
 *
 * To configure set a Java VM option {@code -Dzw_env=} with one of the following values:
 * </p>
 * prod-https : Connection to production with HTTPS
 * </p>
 * prod-http : Connection to production with HTTP
 * </p>
 * staging : Connection to staging
 * </p>
 * test : Connection to test
 * </p>
 * The default is prod-https.
 */
public class ApiConnectionConfiguration {

    static {

        String environment = System.getProperty("zw_env", "prod-https");

        if (environment.equals("prod-http")) {

            API_HOST = ApiConnection.DEFAULT_HOST;
            SIGNALS_HOST = ApiConnection.DEFAULT_SIGNALS_HOST;
            SIGNALS_PORT = ApiConnection.DEFAULT_SIGNALS_PORT;

        } else if (environment.equals("staging")) {

            API_HOST = ApiConnection.STAGING_HOST;
            SIGNALS_HOST = ApiConnection.STAGING_SIGNALS_HOST;
            SIGNALS_PORT = ApiConnection.DEFAULT_SIGNALS_PORT;

        } else if (environment.equals("test")) {

            API_HOST = ApiConnection.TEST_HOST;
            SIGNALS_HOST = ApiConnection.TEST_SIGNALS_HOST;
            SIGNALS_PORT = ApiConnection.DEFAULT_SIGNALS_PORT;

        } else { // DEFAULT: prod-https

            API_HOST = ApiConnection.DEFAULT_HTTPS_HOST;
            SIGNALS_HOST = ApiConnection.DEFAULT_SIGNALS_HOST;
            SIGNALS_PORT = ApiConnection.DEFAULT_SIGNALS_PORT;
        }
    }

    public static String API_HOST;
    public static String SIGNALS_HOST;
    public static int    SIGNALS_PORT;

}
