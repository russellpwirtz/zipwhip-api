package com.zipwhip.api;

import com.zipwhip.api.dto.*;
import com.zipwhip.api.settings.SettingsStore;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.signals.presence.ProductLine;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Implementations of this class provide a high level way to communicate with the Zipwhip web API.
 * The communication takes place over a {@code ApiConnection} connection.
 *
 * @author Michael
 */
public interface ZipwhipClient extends Destroyable {

    /**
     * Send a message via Zipwhip.
     *
     * @param message A {@code Message} object from which to send the message.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Message message) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address Zipwhip {@link Address} scheme.
     * @param body The body of the message to be sent.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Address address, String body) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address Zipwhip {@link Address} scheme.
     * @param body The body of the message to be sent.
     * @param fromName The name of the sender of the message.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Address address, String body, String fromName) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address The address, generally the mobile number, of the message recipient.
     * @param body The body of the message to be sent.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(String address, String body) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address The address, generally the mobile number, of the message recipient.
     * @param body The body of the message to be sent.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Collection<String> address, String body) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address The address, generally the mobile number, of the message recipient.
     * @param body The body of the message to be sent.
     * @param fromName The name of the sender of the message.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Collection<String> address, String body, String fromName) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address The address, generally the mobile number, of the message recipient.
     * @param body The body of the message to be sent.
     * @param fromName The name of the sender of the message.
     * @param advertisement A code indicating to Zipwhip what should be appended to the message.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(Collection<String> address, String body, String fromName, String advertisement) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address The address, generally the mobile number, of the message recipient.
     * @param body The body of the message to be sent.
     * @param fromName The name of the sender of the message.
     * @param advertisement A code indicating to Zipwhip what should be appended to the message.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(String address, String body, String fromName, String advertisement) throws Exception;

    /**
     * Send a message via Zipwhip.
     *
     * @param address The address, generally the mobile number, of the message recipient.
     * @param body The body of the message to be sent.
     * @param fromAddress The send strategy.
     * @return A {@code List} of {@code MessageToken}s, indicating the status of the message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendMessage(String address, String body, int fromAddress) throws Exception;

    /**
     * Create a new group.
     *
     * @param type The type of group, eg. reply-all.
     * @param advertisement A code indicating to Zipwhip what should be appended to the message.
     * @return A {@link Contact} representing the new group.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Contact saveGroup(String type, String advertisement) throws Exception;

    /**
     * Create a new group.
     *
     * @return A {@link Contact} representing the new group.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Contact saveGroup() throws Exception;

    /**
     * Save or update the user's information.
     *
     * @param contact A {@link Contact} object representing the user to be saved.
     * @return The {@link Contact} object representing the user that has been saved.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Contact saveUser(Contact contact) throws Exception;

    /**
     * Returns a Message object
     *
     * @deprecated Use {@code getMessage(Long id)}
     * @param uuid - message uuid
     * @return A Message DTO matching the uuid.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Message getMessage(String uuid) throws Exception;

    /**
     * Returns a Message object
     *
     * @param id - message id
     * @return A Message DTO matching the id.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Message getMessage(Long id) throws Exception;

    /**
     * 
     * @return A list of all {@link Device}s associated with the user.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    public List<Device> listDevices() throws Exception;

    /**
     * 
     * @return A list of the most recent {@link Conversation}s associated with the user, up to the server's predefined limit.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    public List<Conversation> listConversations() throws Exception;

    /**
     *
     * @param limit The maximum limit of how many conversations to return.
     * @return A list of the most recent {@link Conversation}s associated with the user, up to the specified limit.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    public List<Conversation> listConversations(int limit) throws Exception;

    /**
     * @param start Where to start list conversations.  (Zero indexed)
     * @param limit The maximum limit of how many conversations to return.
     * @return A list of the most recent {@link Conversation}s associated with the user, up to the specified limit.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    public List<Conversation> listConversations(int start, int limit) throws Exception;

    /**
     *
     * @return A list of all {@link Contact}s associated with the supplied user.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response, or the server returns a failure message.
     */
    public List<Contact> listContacts() throws Exception;

    /**
     * 
     * @param fingerprint The fingerprint of the conversation that you wish to mark as read.
     * @return A boolean which represents whether or not the operation completed successfully.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    public boolean readConversation(String fingerprint) throws Exception;

    /**
     *
     * @param fingerprint The fingerprint of the conversation that you wish to mark as read.
     * @return A boolean which represents whether or not the operation completed successfully.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    public boolean deleteConversation(String fingerprint) throws Exception;

    /**
     * Returns the contact for the provided contact id.
     *
     * @param contactId The id of the contact to be deleted.
     * @return A boolean which represents whether or not the operation completed successfully.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    public boolean deleteContact(long contactId) throws Exception;

    /**
     * Returns the most recent messages for the user, up to a limit maintained by the zipwhip server.
     * @param fingerprint The fingerprint for which you wish to load messages.
     * @return A list consisting of the most recent messages associated with the supplied fingerprint.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    public List<Message> listMessagesByFingerprint(String fingerprint) throws Exception;

    /**
     * Returns the most recent messages for the supplied conversation, up to a the supplied limit.
     * @param fingerprint The fingerprint for which you wish to load messages.
     * @param limit The maximum number of messages that this call will return.
     * @return A list consisting of the most recent messages associated with the supplied fingerprint.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    public List<Message> listMessagesByFingerprint(String fingerprint, int limit) throws Exception;

    /**
     *
     * @return A list consisting of the most recent messages associated with the user.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    public List<Message> listMessages() throws Exception;

    /**
     * Returns the most recent messages for the user, up to the supplied limit.
     * @param limit The maximum number of messages that this call will return.
     * @return A list consisting of the most recent messages associated with this user.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    public List<Message> listMessages(int limit) throws Exception;

    /**
     * Delete messages by their corresponding UUIDs.
     *
     * @deprecated use {@code readMessage(List id)}
     * @param uuids A list of message uuids to delete.
     * @return True for a successful delete otherwise false.
     * @throws Exception If an error occurred while sending or parsing the response.
     */
    boolean messageRead(List<String> uuids) throws Exception;

    /**
     * Delete messages by their corresponding IDs.
     *
     * @param ids A list of message ids to delete.
     * @return True for a successful delete otherwise false.
     * @throws Exception If an error occurred while sending or parsing the response.
     */
    boolean readMessage(List<Long> ids) throws Exception;

    /**
     * Read messages by their corresponding UUIDs.
     *
     * @deprecated use {@code deleteMessage(List ids)}
     * @param uuids A list of message uuids to mark as read.
     * @return True for a successful read otherwise false.
     * @throws Exception If an error occurred while sending or parsing the response.
     */
    boolean messageDelete(List<String> uuids) throws Exception;

    /**
     * Read messages by their corresponding IDs.
     *
     * @param ids A list of message ids to mark as read.
     * @return True for a successful read otherwise false.
     * @throws Exception If an error occurred while sending or parsing the response.
     */
    boolean deleteMessage(List<Long> ids) throws Exception;

    /**
     * Returns a MessageStatus object
     *
     * @deprecated use {@code getMessageStatus(Long id)}
     * @param uuid - message uuid
     * @return A MessageStatus DTO matching the uuid.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    MessageStatus getMessageStatus(String uuid) throws Exception;

    /**
     * Returns a MessageStatus object
     *
     * @param id - message id
     * @return A MessageStatus DTO matching the id.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    MessageStatus getMessageStatus(Long id) throws Exception;

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
     * @param mobileNumber The mobile number of the contact to get.
     * @return contact The contact corresponding to the mobile number.
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

    /**
     * Send a signal via Zipwhip SignalServer.
     * Generally this is for debug since the SignalServer protocol is proprietary.
     *
     * @param scope The scope of the signal, ie device.
     * @param channel The channel the signal is on.
     * @param event The event of the signal.
     * @param payload The content of the signal.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    void sendSignal(String scope, String channel, String event, String payload) throws Exception;

    /**
     * A debug call to generate a SIGNAL_VERIFICATION command back to the client
     * associated with the {@param clientId}.
     *
     * @param clientId The client ID to send the SIGNAL_VERIFICATION to.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    void sendSignalsVerification(String clientId) throws Exception;

    /**
     * Save a new contact for the user or update an existing contact.
     *
     * @param address The address of the contact, generally mobile number.
     * @param firstName Contact's first name.
     * @param lastName Contact's last name.
     * @param phoneKey  Contact's phone type.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    void saveContact(String address, String firstName, String lastName, String phoneKey) throws Exception;

    /**
     * Save a new contact for the user or update an existing contact.
     *
     * @param address The address of the contact, generally mobile number.
     * @param firstName Contact's first name.
     * @param lastName Contact's last name.
     * @param phoneKey  Contact's phone type.
     * @param notes Free text.
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    void saveContact(String address, String firstName, String lastName, String phoneKey, String notes) throws Exception;

    /**
     * Save a new contact for the user or update an existing contact.
     *
     * @param address The address of the contact, generally mobile number.
     * @param firstName Contact's first name.
     * @param lastName Contact's last name.
     * @param phoneKey  Contact's phone type.
     * @param notes Free text.
     * @param location Contact's location
     * @param email Contact's e-mail address.                 
     * @throws Exception if an error occurs communicating with Zipwhip
     */
    void saveContact(String address, String firstName, String lastName, String phoneKey, String notes, String location, String email) throws Exception;
    
    /**
     * Add a member to an existing group.
     *
     * @param groupAddress The address of the group to add a new member to.
     * @param contactAddress The address, mobile number, of the new contact.
     * @return A {@link Contact} representing the new group member.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Contact addMember(String groupAddress, String contactAddress) throws Exception;

    /**
     * Add a member to an existing group.
     *
     * @param groupAddress The address of the group to add a new member to.
     * @param contactAddress The address, mobile number, of the new contact.
     * @param firstName Contact's first name.
     * @param lastName Contact's last name.
     * @param phoneKey  Contact's phone type.
     * @param notes Free text.
     * @return A {@link Contact} representing the new group member.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Contact addMember(String groupAddress, String contactAddress, String firstName, String lastName, String phoneKey, String notes) throws Exception;

    /**
     * Toggles the on/off value of Device Carbon in the cloud. The cloud holds the master value that Device Carbon uses
     * to override any other value it has.
     *
     * @param enabled: turn Device Carbon on/off in the cloud
     * @param versionCode: What version of Device Carbon is being used?
     * @throws Exception if an error occurs communicating with Zipwhip.
     */
    void carbonEnable(boolean enabled, Integer versionCode) throws Exception;

    /**
     * Returns the on/off state Device Carbon should be in according to the cloud.
     *
     * @param enabled: The on/off state Device Carbon is currently in
     * @return What state the cloud thinks device carbon is in
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Boolean carbonEnabled(boolean enabled, Integer versionCode) throws Exception;

    /**
     * Register Device Carbon for Push Notifications from Google
     * @param registrationId - Google provided registrationId to send push notifications too
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    Boolean carbonRegister(String registrationId) throws Exception;

    /**
     * Informs Zipwhip of Device Carbon usage statistics
     * @param totalPhoneMessages - total messages sent/receive on the device
     * @throws Exception
     */
    void carbonStats(int totalPhoneMessages) throws Exception;

    /**
     * Initiates the signup process to:
     * 1) Enroll a new account if one doesn't exist
     * 2) Create necessary subscriptions
     * 3) Eventually return a valid session key for this device
     *
     * @param mobileNumber: mobile number of the account
     * @param portal: product that is retrieving a session
     * @return clientId that is used to finish the challenge process
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    String sessionChallenge(String mobileNumber, String portal) throws Exception;

    /**
     * Finishes the challenge process and returns a session key
     *
     * @param clientId: clientId returned by the original sessionChallenge call
     * @param securityToken: The random string that is sent in an ".signup verify" sms to the phone
     * @param arguments: any extra arguments for the cloud to react to (.signup devicecarbonall)
     * @param userAgent: Device's user agent
     * @return A session key
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    String sessionChallengeConfirm(String clientId, String securityToken, String arguments, String userAgent) throws Exception;

    /**
     *
     * @param packageName
     * @return
     * @throws Exception
     */
    boolean userUnenroll(String packageName) throws Exception;

    /**
     * Query Zipwhip Face Ecosystem for a user's preferred profile name.
     *
     * @param mobileNumber The mobile number of the user you wish to query.
     * @return The user's full name if it exists or empty string.
     * @throws Exception if an error occurs communicating with Zipwhip or parsing the response.
     */
    String getFaceName(String mobileNumber) throws Exception;

    /**
     * Query Zipwhip Face Ecosystem for a user's preferred profile image.
     *
     * @param mobileNumber The mobile number of the user you wish to query.
     * @param thumbnail true if you want a thumbnail, false for the full image
     * @return A byte[] of the user's image.
     * @throws Exception if an error occurs communicating with Zipwhip or the image is not found.
     */
    byte[] getFaceImage(String mobileNumber, boolean thumbnail) throws Exception;

    /**
     * Query Zipwhip Face Ecosystem for a user's preferred profile image.
     *
     * @param mobileNumber The mobile number of the user you wish to query.
     * @param size the size of thumnail in pixels
     * @return A byte[] of the user's image.
     * @throws Exception if an error occurs communicating with Zipwhip or the image is not found.
     */
    byte[] getFaceImage(String mobileNumber, int size) throws Exception;

    /**
     * The API for reporting metrics events on Zipwhip products.
     *
     * @param product The product type for which we are recording this event.
     * @param mobileNumber Cleaned mobile number for this product.
     * @param event The event which we are recording.
     * @param payload Optional extra text to record with the event.
     * @throws Exception if an error occurs communicating with Zipwhip or if required parameters are missing.
     */
    void recordMetricsEvent(ProductLine product, String mobileNumber, String event, String payload) throws Exception;

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
    ApiConnection getConnection();

    /**
     *
     * @param connection the connection to use
     */
    void setConnection(ApiConnection connection);

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
