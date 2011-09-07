package com.zipwhip.api;

import com.zipwhip.api.dto.Contact;
import com.zipwhip.api.dto.Conversation;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalObserverAdapter;
import com.zipwhip.api.signals.commands.Message;
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
    public static final String TO_NUMBER = "2069308934";

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

        Connection connection = ConnectionFactory.newInstance().username(USERNAME).password(PASSWORD).create();
        ZipwhipClient client = new DefaultZipwhipClient(connection);

        // Build a Presence object
        Presence presence = new Presence.Builder().ip("10.168.1.23").category(PresenceCategory.Phone).isConnected(true).build();

        // Connect to SignalServer passing in our presence
        client.connect(presence);

        // Use SignalObserverAdapter so you can choose the signals you are interested in observing
        client.addSignalObserver(new SignalObserverAdapter() {

            @Override
            public void notifyContact(Signal signal, Contact contact) {
                logger.debug(contact.toString());
            }

            @Override
            public void notifyConversation(Signal signal, Conversation conversation) {
                logger.debug(conversation.toString());
            }

            @Override
            public void notifyMessage(Signal signal, Message message) {
            }
        });

        // Let's send a message!
        client.sendMessage(TO_NUMBER, "Hello World");

        // And wait for signals to come in...
        while (true) {

        }
    }

}
