package com.zipwhip.api;

import com.zipwhip.api.dto.Contact;
import com.zipwhip.api.dto.Message;
import com.zipwhip.api.dto.MessageStatus;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.api.settings.SettingsStore;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.events.Observer;
import com.zipwhip.lib.Address;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 * This class operates on a Connection.
 *
 * @author Michael
 */
public interface ZipwhipClient extends Destroyable {

    List<MessageToken> sendMessage(Message message) throws Exception;

    List<MessageToken> sendMessage(Address address, String body) throws Exception;

    List<MessageToken> sendMessage(Address address, String body, String fromName) throws Exception;

    List<MessageToken> sendMessage(String address, String body) throws Exception;

    List<MessageToken> sendMessage(Collection<String> address, String body) throws Exception;

    List<MessageToken> sendMessage(Collection<String> address, String body, String fromName) throws Exception;

    List<MessageToken> sendMessage(Collection<String> address, String body, String fromName, String advertisement) throws Exception;

    List<MessageToken> sendMessage(String address, String body, String fromName, String advertisement) throws Exception;

    Contact saveGroup(String type, String advertisement) throws Exception;

    Contact saveGroup() throws Exception;

    Contact saveUser(Contact contact) throws Exception;

    /**
     * Returns a Message object
     *
     * @param uuid - message uuid
     * @return A Message DTO matching the uuid.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    Message getMessage(String uuid) throws Exception;

    /**
     * Delete messages by their corresponding UUIDs.
     *
     * @param uuids A list of message uuids to delete.
     * @return True for a successful delete otherwise false.
     */
    boolean messageRead(List<String> uuids) throws Exception;

    /**
     * Read messages by their corresponding UUIDs.
     *
     * @param uuids A list of message uuids to mark as read.
     * @return True for a successful read otherwise false.
     */
    boolean messageDelete(List<String> uuids) throws Exception;

    /**
     * Returns a MessageStatus object
     *
     * @param uuid - message uuid
     * @return A MessageStatus DTO matching the uuid.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    MessageStatus getMessageStatus(String uuid) throws Exception;

    /**
     * Returns the contact for the provided contact id.
     *
     * @param id The id of the contact.
     * @return A Connect DTO matching the id.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    Contact getContact(long id) throws Exception;

    /**
     * Returns the contact for the provided mobile number.
     *
     * @param mobileNumber
     * @return contact
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    Contact getContact(String mobileNumber) throws Exception;

    /**
     * Query for the list of presences for your session
     *
     * @param category The category of presence to query for
     * @return List of presences for category and session
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    List<Presence> getPresence(PresenceCategory category) throws Exception;

    void sendSignal(String scope, String channel, String event, String payload) throws Exception;

    void saveContact(String address, String firstName, String lastName, String phoneKey) throws Exception;

    void saveContact(String address, String firstName, String lastName, String phoneKey, String notes) throws Exception;

    Contact addMember(String groupAddress, String contactAddress) throws Exception;

    Contact addMember(String groupAddress, String contactAddress, String firstName, String lastName, String phoneKey, String notes) throws Exception;

    /**
     * Connect to Zipwhip Signals if setup.
     *
     * @param presence a Presence object to pass to the SignalServer
     * @throws Exception any connection problem
     * @return so you can wait until login succeeds
     */
    Future<Boolean> connect(Presence presence) throws Exception;

    /**
     * Connect to Zipwhip Signals if setup.
     *
     * @throws Exception any connection problem
     * @return so you can wait until login succeeds
     */
    Future<Boolean> connect() throws Exception;

    /**
     * Tell the SignalProvider to disconnect from the server.
     *
     * @return an event that tells you its complete
     * @throws Exception if an I/O happens while disconnecting
     */
    Future<Void> disconnect() throws Exception;

    /**
     * Listen for signals. This is a convenience method
     *
     * @param observer An observer object to receive callbacks on
     */
    void addSignalObserver(Observer<List<Signal>> observer);

    /**
     * Listen for connection changes. This is a convenience method
     *
     * This observer will be called if:
     *
     * We lose our TCP/IP connection to the SignalServer
     *
     * @param observer An observer object to receive callbacks on
     */
    void addSignalsConnectionObserver(Observer<Boolean> observer);


    /**
     * A connection to Zipwhip over a medium.
     *
     * @return the current connection
     */
    Connection getConnection();

    /**
     *
     * @param connection the connection to use
     */
    void setConnection(Connection connection);

    /**
     * Getter for the SignalProvider. SignalProvider manages the connection to the SignalServer
     * and provides events when messages are received or connection state changes.
     *
     * @return An implementation of SignalProvider or null if none has been set.
     */
    SignalProvider getSignalProvider();

    /**
     * Setter for the SignalProvider. SignalProvider manages the connection to the SignalServer
     * and provides events when messages are received or connection state changes.
     *
     * @param provider An implementation of SignalProvider.
     */
    void setSignalProvider(SignalProvider provider);

    /**
     * Get the setting settingsStore
     *
     * @return the setting settingsStore
     */
    SettingsStore getSettingsStore();

    /**
     * Set the setting settingsStore
     *
     * @param store the setting settingsStore
     */
    void setSettingsStore(SettingsStore store);

}
