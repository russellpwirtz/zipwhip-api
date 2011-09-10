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

    public enum SignalType {

        CONTACT,
        CONVERSATION,
        DEVICE,
        MESSAGE,
        MESSAGEPROGRESS,
        CARBON,
        NOVALUE,
        UNKNOWN;

        public static SignalType toSignalType(String typeString) {

            if (StringUtil.isNullOrEmpty(typeString)) {
                return NOVALUE;
            }

            // Cleanup the type string
            String enumString = typeString.toUpperCase();

            try {

                return valueOf(enumString);

            } catch (Exception ex) {

                return UNKNOWN;
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

            logger.debug("Received signal of TYPE: " + signal.getType());

            switch (SignalType.toSignalType(signal.getType())) {

                case MESSAGE:
                     notifyMessage(signal, (Message) signal.getContent());
                     break;

                case MESSAGEPROGRESS:
                    notifyMessageProgress(signal, (MessageProgress) signal.getContent());
                     break;

                case CONTACT:
                    notifyContact(signal, (Contact) signal.getContent());
                     break;

                case CONVERSATION:
                    notifyConversation(signal, (Conversation) signal.getContent());
                    break;

                case CARBON:
                    notifyCarbonEvent(signal, (CarbonEvent) signal.getContent());
                    break;

                case DEVICE:
                    notifyDevice(signal, (Device) signal.getContent());
                    break;

                default:
                    logger.warn("No match found for TYPE: " + signal.getType());
                    notifyUnrecognised(signal);
                    break;
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

    public void notifyUnrecognised(Signal signal) {
        logger.debug("notifyUnrecognised - Not implemented");
    }

}
