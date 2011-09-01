package com.zipwhip.api;

import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 * An example application to demonstrate connecting to Zipwhip and sending and
 * receiving messages.
 * 
 */
public class App {

    private static Logger logger = Logger.getLogger(App.class);

    public static final String USERNAME = "4252466003";
    public static final String PASSWORD = "zipwhip1";

    public static void main(String[] args) {

        // Configure basic console logging
        BasicConfigurator.configure();

        try {
            startApp();
        } catch (Exception e) {
            logger.error("Error connecting", e);
        }
    }
    
    private static void startApp() throws Exception {

        //ZipwhipClient client = ZipwhipClientFactory.createViaUsername(USERNAME, PASSWORD);
        Connection connection = HttpConnectionFactory.getInstance().setUsername(USERNAME).setPassword(PASSWORD).create();
        ZipwhipClient client = new DefaultZipwhipClient(connection);

        Presence presence = new Presence.Builder().ip("10.168.1.23").category(PresenceCategory.Phone).build();

        client.connect(presence);

        client.sendMessage("2069308934", "Yo");

        while (true) {

        }
    }

}
