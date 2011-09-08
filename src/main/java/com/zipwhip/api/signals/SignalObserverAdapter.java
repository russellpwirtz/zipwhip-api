package com.zipwhip.api.signals;

import com.zipwhip.api.dto.*;
import com.zipwhip.events.Observer;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/6/11
 * Time: 5:22 PM
 * <p/>
 * This is a convenience class for translating a generic Signal into a Zipwhip DTO.
 * <p/>
 * Use this when calling ZipwhipClient.addSignalObserver()
 */
public abstract class SignalObserverAdapter implements Observer<List<Signal>> {

    private static Logger logger = Logger.getLogger(SignalObserverAdapter.class);

    public enum SignalUri {

        SIGNAL_MESSAGE_RECEIVE,
        SIGNAL_MESSAGE_PROGRESS,
        SIGNAL_MESSAGE_READ,
        SIGNAL_MESSAGE_DELETE,
        SIGNAL_MESSAGE_SEND,

        SIGNAL_MESSAGEPROGRESS_MESSAGEPROGRESS,

        SIGNAL_CONVERSATION_CHANGE,

        SIGNAL_CONTACT_NEW,
        SIGNAL_CONTACT_SAVE,
        SIGNAL_CONTACT_DELETE,
        SIGNAL_CONTACT_CHANGE,

        SIGNAL_CARBON_PROXY,
        SIGNAL_CARBON_READ,
        SIGNAL_CARBON_DELETE,

        NOVALUE;

        public static SignalUri toSignalUri(String uriString) {

            if (StringUtil.isNullOrEmpty(uriString)) {
                return NOVALUE;
            }

            // Cleanup the URI string first, get rid of first slash
            String enumString = uriString.substring(1, uriString.length());

            // Convert slashes to underscores
            enumString = enumString.replaceAll("/", "_");

            // All upper case
            enumString = enumString.toUpperCase();

            try {

                return valueOf(enumString);

            } catch (Exception ex) {

                return NOVALUE;
            }
        }
    }

    /**
     * This method is final as it does the adaptation from Signal to DTO.
     *
     * @param sender  The sender might not be the same object every time, so we'll let it just be object.
     * @param signals A list of signals we are being notified about.
     */
    @Override
    final public void notify(Object sender, List<Signal> signals) {

        for (Signal signal : signals) {

            logger.debug("Received signal on URI: " + signal.getUri());

            switch (SignalUri.toSignalUri(signal.getUri())) {

                case SIGNAL_MESSAGE_RECEIVE:
                case SIGNAL_MESSAGE_PROGRESS:
                case SIGNAL_MESSAGE_SEND:
                case SIGNAL_MESSAGE_READ:
                case SIGNAL_MESSAGE_DELETE:
                     notifyMessage(signal, (Message) signal.getContent());
                     break;

                case SIGNAL_MESSAGEPROGRESS_MESSAGEPROGRESS:
                    notifyMessageProgress(signal, (MessageProgress) signal.getContent());
                     break;

                case SIGNAL_CONTACT_CHANGE:
                case SIGNAL_CONTACT_DELETE:
                case SIGNAL_CONTACT_NEW:
                case SIGNAL_CONTACT_SAVE:
                    notifyContact(signal, (Contact) signal.getContent());
                     break;

                case SIGNAL_CONVERSATION_CHANGE:
                    notifyConversation(signal, (Conversation) signal.getContent());
                    break;

                case SIGNAL_CARBON_PROXY:
                case SIGNAL_CARBON_READ:
                case SIGNAL_CARBON_DELETE:
                    notifyCarbonEvent(signal, (CarbonEvent) signal.getContent());
                    break;

                default:
                    logger.warn("No match found for URI: " + signal.getUri());
            }
        }
    }

    public void notifyContact(Signal signal, Contact contact) {
        logger.debug("notifyContact - Not implemented");
    }

    public void notifyConversation(Signal signal, Conversation conversation) {
        logger.debug("notifyConversation - Not implemented");
    }

    public void notifyDevice(Signal signal, Device device) {
        logger.debug("notifyDevice - Not implemented");
    }

    public void notifyMessage(Signal signal, Message message) {
        logger.debug("notifyMessage - Not implemented");
    }

    public void notifyMessageProgress(Signal signal, MessageProgress messageProgress) {
        logger.debug("notifyMessageProgress - Not implemented");
    }

    public void notifyCarbonEvent(Signal signal, CarbonEvent carbonEvent) {
        logger.debug("notifyCarbonEvent - Not implemented");
    }

}
