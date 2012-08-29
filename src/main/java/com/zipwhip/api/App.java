package com.zipwhip.api;

import com.zipwhip.api.dto.*;
import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalObserverAdapter;
import com.zipwhip.events.Observer;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceBuilder;
import com.zipwhip.signals.presence.PresenceCategory;
import org.apache.log4j.Logger;

import java.util.concurrent.Future;

/**
 * An example application to demonstrate connecting to Zipwhip and sending and
 * receiving messages.
 * 
 */
public class App {

    private static Logger LOGGER = Logger.getLogger(App.class);

    public static final String USERNAME = "2063758020";
    public static final String PASSWORD = "zipwhip1";
    public static final String TO_NUMBER = "2063758020";

    public static void main(String[] args) {

        // Configure basic console logging
//        BasicConfigurator.configure();

        try {
            startApp();
        } catch (Exception e) {
            LOGGER.error("Error connecting", e);
        }
    }
    
    private static void startApp() throws Exception {

        // Fire-up a new authenticated client
        ZipwhipClient client;

        client = ZipwhipClientFactory.createViaUsername(USERNAME, PASSWORD);

        /**
         * Alternatively you can create a client from a sessionKey
         *
         * client = ZipwhipClientFactory.createViaSessionKey("775a21d3-ed22-439d-a5c4-b08decaa9556:132961202");
         */

        /**
         * If you want a settings store implementation other than the basic,
         * one user Java implementation you can set your custom store like this:
         *
         * client.setSettingsStore(new MemorySettingStore());
         */

        // Use SignalObserverAdapter so you can choose the signals you are interested in observing
        client.addSignalObserver(new SignalObserverAdapter() {

            @Override
            public void notifyContact(Signal signal, Contact contact) {
                LOGGER.debug(contact.toString());
            }

            @Override
            public void notifyConversation(Signal signal, Conversation conversation) {
                LOGGER.debug(conversation.toString());
            }

            @Override
            public void notifyDevice(Signal signal, Device device) {
                LOGGER.debug(device.toString());
            }

            @Override
            public void notifyMessage(Signal signal, Message message) {
                LOGGER.debug(message.toString());
            }

            @Override
            public void notifyCarbonEvent(Signal signal, CarbonEvent carbonEvent) {
                LOGGER.debug(carbonEvent.toString());
            }
        });

        // Observe clientId change events
        client.getSignalProvider().onNewClientIdReceived(new Observer<String>() {
            @Override
            public void notify(Object sender, String item) {
                LOGGER.debug("APP::Received a new clientId " + item);
            }
        });

        // Observe Signal Verification events
        client.getSignalProvider().onSignalVerificationReceived(new Observer<Void>() {
            @Override
            public void notify(Object sender, Void item) {
                LOGGER.debug("APP::Received a signal verification");
            }
        });

        // Observe connection change events
        client.getSignalProvider().onConnectionChanged(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                LOGGER.debug("APP::Received a connection change to " + (item ? "CONNECTED" : "DISCONNECTED"));
            }
        });

        // Observe the presence of the phone
        client.getSignalProvider().onPhonePresenceReceived(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                LOGGER.debug("APP::PHONE Presence reported as " + (item ? "CONNECTED" : "DISCONNECTED"));
            }
        });

        // Observe a PING being sent to the SignalServer indicating that the line has been inactive for a period of time
        client.getSignalProvider().onPingEvent(new Observer<PingEvent>() {
            @Override
            public void notify(Object sender, PingEvent item) {
                LOGGER.debug("APP::PING_EVENT " + item.toString());
            }
        });

        // Build a Presence object
        Presence presence = new PresenceBuilder().ip("10.168.1.23").category(PresenceCategory.Phone).isConnected(true).build();

        // Connect to SignalServer passing in our presence
        Future<Boolean> connectTask = client.connect(presence);

        if(connectTask.get()) {

             // We're connected, let's send a message!
            client.sendMessage(TO_NUMBER, "Hello World");

            // And wait for signals to come in...
            while (true) {}
        }
    }

}
