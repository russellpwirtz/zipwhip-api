package com.zipwhip.api;

import com.zipwhip.api.dto.*;
import com.zipwhip.api.exception.NotAuthenticatedException;
import com.zipwhip.api.response.BooleanServerResponse;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.response.StringServerResponse;
import com.zipwhip.api.settings.SettingsStore;
import com.zipwhip.api.signals.*;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.signals.presence.ProductLine;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;

import java.util.*;
import java.util.concurrent.*;

/**
 * Date: Jul 17, 2009 Time: 7:25:37 PM
 * <p/>
 * This provides an Object Oriented way to access the Zipwhip API. It uses a
 * Connection internally for low-level Zipwhip access. This class does not
 * manage your authentication, the Connection abstracts this away from
 * the "Zipwhip" class.
 */
public class DefaultZipwhipClient extends ClientZipwhipNetworkSupport implements ZipwhipClient {

    /**
     * Create a new DefaultZipwhipClient.
     *
     * @param connection     The connection to Zipwhip API
     * @param signalProvider The connection client for Zipwhip SignalServer.
     */
    public DefaultZipwhipClient(ApiConnection connection, SignalProvider signalProvider) {

        super(connection, signalProvider);

        // Start listening to provider events that interest us
        initSignalProviderEvents();
    }

    private void initSignalProviderEvents() {

        signalProvider.onNewClientIdReceived(new Observer<String>() {
            @Override
            public void notify(Object sender, String clientId) {

                if (StringUtil.isNullOrEmpty(clientId)) {
                    LOGGER.warn("Received CONNECT without clientId");
                    return;
                }

                if (StringUtil.isNullOrEmpty(connection.getSessionKey())) {
                    settingsStore.put(SettingsStore.Keys.CLIENT_ID, clientId);
                    return;
                }

                String managedClientId = settingsStore.get(SettingsStore.Keys.CLIENT_ID);

                if (StringUtil.exists(managedClientId)) {

                    // clientId changed, unsubscribe the old one, and sub the new one
                    if (!managedClientId.equals(clientId)) {

                        settingsStore.clear();

                        settingsStore.put(SettingsStore.Keys.CLIENT_ID, clientId);

                        // Do a disconnect then connect
                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put("clientId", clientId);
                        params.put("sessions", connection.getSessionKey());

                        try {
                            executeSync(SIGNALS_DISCONNECT, params);

                            executeSync(SIGNALS_CONNECT, params);

                        } catch (Exception e) {
                            LOGGER.error("Error calling signals/connect", e);
                        }
                    }
                } else {

                    settingsStore.put(SettingsStore.Keys.CLIENT_ID, clientId);

                    // lets do a signals connect!
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put("clientId", clientId);
                    params.put("sessions", connection.getSessionKey());

                    try {

                        executeSync(SIGNALS_CONNECT, params);

                    } catch (Exception e) {

                        LOGGER.error("Error calling signals/connect", e);
                    }
                }
            }
        });

        signalProvider.onVersionChanged(new Observer<VersionMapEntry>() {
            @Override
            public void notify(Object sender, VersionMapEntry item) {
                versionsStore.set(item.getKey(), item.getValue());
            }
        });

    }

    @Override
    public Future<Boolean> connect() throws Exception {
        return connect(null);
    }

    @Override
    public Future<Boolean> connect(Presence presence) throws Exception {

        // we need to determine if we're authenticated enough
        if (!connection.isConnected() || !connection.isAuthenticated()) {
            throw new NotAuthenticatedException("The connection cannot operate at this time");
        }

        String managedClientId = settingsStore.get(SettingsStore.Keys.CLIENT_ID);

        // If the clientId has changed we need to invalidate the settings data
        if (StringUtil.isNullOrEmpty(managedClientId) || (StringUtil.exists(signalProvider.getClientId()) && !managedClientId.equals(signalProvider.getClientId()))) {

            LOGGER.debug("ClientId has changed, resetting client id in settings store");

            settingsStore.clear();

            if (signalProvider!=null && signalProvider.getClientId() != null) {
                settingsStore.put(SettingsStore.Keys.CLIENT_ID, signalProvider.getClientId());
            }
        }

        // If the sessionKey has changed we need to invalidate the settings data
        if (StringUtil.exists(connection.getSessionKey()) && !connection.getSessionKey().equals(settingsStore.get(SettingsStore.Keys.SESSION_KEY))) {

            LOGGER.debug("New or changed sessionKey, resetting session key in settings store");

            settingsStore.clear();

            settingsStore.put(SettingsStore.Keys.SESSION_KEY, connection.getSessionKey());
        }

        // Will NOT block until you're connected it's asynchronous
        return signalProvider.connect(settingsStore.get(SettingsStore.Keys.CLIENT_ID), versionsStore.get(), presence);
    }

    @Override
    public Future<Void> disconnect() throws Exception {

        if (!connection.isConnected()) {
            throw new Exception("The connection is not connected!");
        }

        return signalProvider.disconnect();
    }

    @Override
    public List<MessageToken> sendMessage(Address address, String body) throws Exception {
        return sendMessage(Arrays.asList(address.toString()), body, null, null);
    }

    @Override
    public List<MessageToken> sendMessage(Address address, String body, String fromName) throws Exception {
        return sendMessage(Arrays.asList(address.toString()), body, fromName, null);
    }

    @Override
    public List<MessageToken> sendMessage(String address, String body) throws Exception {
        return sendMessage(Arrays.asList(address), body);
    }

    @Override
    public List<MessageToken> sendMessage(Message message) throws Exception {
        return sendMessage(Arrays.asList(message.getAddress()), message.getBody(), message.getFromName(), message.getAdvertisement());
    }

    @Override
    public List<MessageToken> sendMessage(Collection<String> address, String body) throws Exception {
        return sendMessage(address, body, null, null);
    }

    @Override
    public List<MessageToken> sendMessage(Collection<String> address, String body, String fromName) throws Exception {
        return sendMessage(address, body, fromName, null);
    }

    @Override
    public List<MessageToken> sendMessage(Collection<String> addresses, String body, String fromName, String advertisement) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("contacts", addresses);
        params.put("body", body);
        params.put("fromName", fromName);
        params.put("fromAddress", "0");
        params.put("advertisement", advertisement);

        return responseParser.parseMessageTokens(executeSync(MESSAGE_SEND, params));
    }

    @Override
    public List<MessageToken> sendMessage(String address, String body, String fromName, String advertisement) throws Exception {
        return sendMessage(Arrays.asList(address), body, fromName, advertisement);
    }

    @Override
    public List<MessageToken> sendMessage(String address, String body, int fromAddress) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("contacts", address);
        params.put("body", body);
        params.put("fromAddress", fromAddress);

        return responseParser.parseMessageTokens(executeSync(MESSAGE_SEND, params));
    }

    @Deprecated
    @Override
    public Message getMessage(String uuid) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("uuid", uuid);

        return responseParser.parseMessage(executeSync(MESSAGE_GET, params));
    }

    @Override
    public Message getMessage(Long id) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);

        return responseParser.parseMessage(executeSync(MESSAGE_GET, params));
    }

    @Override
    public List<Device> listDevices() throws Exception {
        return responseParser.parseDevices(executeSync(DEVICE_LIST, new HashMap<String, Object>()));
    }

    @Override
    public List<Conversation> listConversations() throws Exception {
        return responseParser.parseConversations(executeSync(CONVERSATION_LIST, new HashMap<String, Object>()));
    }

    @Override
    public List<Conversation> listConversations(int limit) throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", Integer.toString(limit));
        return responseParser.parseConversations(executeSync(CONVERSATION_LIST, params));
    }

    @Override
    public List<Conversation> listConversations(int start, int limit) throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("start", Integer.toString(start));
        params.put("limit", Integer.toString(limit));
        return responseParser.parseConversations(executeSync(CONVERSATION_LIST, params));
    }

    @Override
    public List<Contact> listContacts() throws Exception {
        return responseParser.parseContacts(executeSync(CONTACT_LIST, new HashMap<String, Object>()));
    }

    @Override
    public boolean readConversation(String fingerprint) throws Exception {
        if (StringUtil.isNullOrEmpty(fingerprint)) {
            return false;
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("fingerprint", fingerprint);

        return success(executeSync(CONVERSATION_READ, params));
    }

    @Override
    public boolean deleteConversation(String fingerprint) throws Exception {
        if (StringUtil.isNullOrEmpty(fingerprint)) {
            return false;
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("fingerprint", fingerprint);

        return success(executeSync(CONVERSATION_DELETE, params));
    }

    @Override
    public boolean deleteContact(long contactId) throws Exception {
        if (contactId <= 0){
            return false;
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("contact", Long.toString(contactId));

        return success(executeSync(CONTACT_DELETE, params));
    }

    @Override
    public List<Message> listMessagesByFingerprint(String fingerprint) throws Exception {

        if (StringUtil.isNullOrEmpty(fingerprint)){
            throw new Exception("Attempting to call listMessagesByFingerprint with a null or empty fingerprint.");
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("fingerprint", fingerprint);

        return responseParser.parseMessagesFromConversation(executeSync(CONVERSATION_GET, params));
    }


    @Override
    public List<Message> listMessagesByFingerprint(String fingerprint, int limit) throws Exception {
        if (StringUtil.isNullOrEmpty(fingerprint)){
            throw new Exception("Attempting to call listMessagesByFingerprint with a null or empty fingerprint.");
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("fingerprint", fingerprint);
        params.put("limit", Integer.toString(limit));

        return responseParser.parseMessagesFromConversation(executeSync(CONVERSATION_GET, params));
    }

    @Override
    public List<Message> listMessages() throws Exception {
        return responseParser.parseMessages(executeSync(MESSAGE_LIST, new HashMap<String, Object>()));
    }

    @Override
    public List<Message> listMessages(int limit) throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", Integer.toString(limit));
        
        return responseParser.parseMessages(executeSync(MESSAGE_LIST, params));
    }

    @Deprecated
    @Override
    public boolean messageRead(List<String> uuids) throws Exception {

        if (CollectionUtil.isNullOrEmpty(uuids)) {
            return false;
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("uuid", uuids);

        return success(executeSync(MESSAGE_READ, params));
    }

    @Override
    public boolean readMessage(List<Long> ids) throws Exception {

        if (CollectionUtil.isNullOrEmpty(ids)) {
            return false;
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("message", ids);

        return success(executeSync(MESSAGE_READ, params));
    }

    @Deprecated
    @Override
    public boolean messageDelete(List<String> uuids) throws Exception {

        if (CollectionUtil.isNullOrEmpty(uuids)) {
            return false;
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("uuids", uuids);

        return success(executeSync(MESSAGE_DELETE, params));
    }

    @Override
    public boolean deleteMessage(List<Long> ids) throws Exception {

        if (CollectionUtil.isNullOrEmpty(ids)) {
            return false;
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("message", ids);

        return success(executeSync(MESSAGE_DELETE, params));
    }

    @Deprecated
    @Override
    public MessageStatus getMessageStatus(String uuid) throws Exception {

        Message message = getMessage(uuid);

        if (message == null) {
            return null;
        }

        return new MessageStatus(message);
    }

    @Override
    public MessageStatus getMessageStatus(Long id) throws Exception {

        Message message = getMessage(id);

        if (message == null) {
            return null;
        }

        return new MessageStatus(message);
    }

    @Override
    public Contact getContact(long id) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("id", Long.toString(id));

        return responseParser.parseContact(executeSync(CONTACT_GET, params));
    }

    @Override
    public Contact getContact(String mobileNumber) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("mobileNumber", mobileNumber);

        return responseParser.parseContact(executeSync(CONTACT_GET, params));
    }



    @Override
    public List<Presence> getPresence(PresenceCategory category) throws Exception {

        Map<String, Object> params = new HashMap<String, Object>();

        if (!category.equals(PresenceCategory.NONE)) {
            params.put("category", category.toString());
        }

        return responseParser.parsePresence(executeSync(PRESENCE_GET, params));
    }

    @Override
    public void sendSignal(String scope, String channel, String event, String payload) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        // is this a device signal or a session signal?
        params.put("scope", scope);
        // what happened? was something created? destroyed? changed?
        params.put("event", event);
        // what is the data you need sent to the browser
        params.put("payload", payload);
        // what URI would you like the browser to fire on? Make sure you have browser code that is subscribing to it.
        params.put("channel", channel);
        // send the 3rd party signal.

        executeSync(SIGNAL_SEND, params);
    }

    @Override
    public void sendSignalsVerification(String clientId) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("clientId", clientId);

        executeSync(SIGNALS_VERIFY, params);
    }

    @Override
    public Contact addMember(String groupAddress, String contactAddress) throws Exception {
        return addMember(groupAddress, contactAddress, null, null, null, null);
    }

    @Override
    public Contact addMember(String groupAddress, String contactAddress, String firstName, String lastName, String phoneKey, String notes) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("firstName", firstName);
        params.put("lastName", lastName);
        params.put("phoneKey", phoneKey);
        params.put("notes", notes);
        params.put("group", groupAddress);
        params.put("mobileNumber", new Address(contactAddress).getAuthority());

        ServerResponse serverResponse = executeSync(GROUP_ADD_MEMBER, params);

        return responseParser.parseContact(serverResponse);

    }

    @Override
    public void carbonEnable(boolean enabled, Integer versionCode) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("enabled", enabled);

        if(versionCode != null) {
            params.put("version", versionCode.toString());
        }

        executeSync(CARBON_ENABLE, params);

    }

    @Override
    public Boolean carbonEnabled(boolean enabled, Integer versionCode) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("enabled", enabled);

        if(versionCode != null) {
            params.put("version", versionCode.toString());
        }

        ServerResponse response = executeSync(CARBON_ENABLED, params);

        if (response instanceof BooleanServerResponse) {
            BooleanServerResponse booleanServerResponse = (BooleanServerResponse) response;

            return booleanServerResponse.getResponse();

        } else {
            throw new Exception("Unrecognized server response for carbon enabled");
        }

    }

    @Override
    public Boolean carbonRegister(String registrationId) throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();

        if(!StringUtil.isNullOrEmpty(registrationId)) {
            params.put("registrationId", registrationId);
        }

        return success(executeSync(CARBON_REGISTER, params));

    }

    @Override
    public void carbonStats(int totalPhoneMessages) throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("messageCount", totalPhoneMessages);

        executeSync(CARBON_STATS, params);
    }


    @Override
    public String sessionChallenge(String mobileNumber, String portal) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("mobileNumber", mobileNumber);
        params.put("portal", portal);

        ServerResponse response = executeSync(CHALLENGE_REQUEST, params, false);

        if (response instanceof StringServerResponse) {
            StringServerResponse stringServerResponse = (StringServerResponse) response;
            return stringServerResponse.response;
        } else {
            throw new Exception("Unrecognized server response for challenge request");
        }

    }

    @Override
    public String sessionChallengeConfirm(String clientId, String securityToken, String arguments, String userAgent) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("clientId", clientId);
        params.put("securityToken", securityToken);
        params.put("arguments", arguments);
        params.put("userAgent", userAgent);

        ServerResponse response = executeSync(CHALLENGE_CONFIRM, params, false);

        if (response instanceof StringServerResponse) {
            StringServerResponse stringServerResponse = (StringServerResponse) response;
            return stringServerResponse.response;
        } else {
            throw new Exception("Unrecognized server response for challenge confirm");
        }

    }

    @Override
    public boolean userUnenroll(String packageName) throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("package", packageName);

        return success(executeSync(USER_UNENROLL, params));

    }

    @Override
    public void addSignalObserver(Observer<List<Signal>> observer) {
        getSignalProvider().onSignalReceived(observer);
    }

    @Override
    public void addSignalsConnectionObserver(Observer<Boolean> observer) {
        getSignalProvider().onConnectionChanged(observer);
    }

    @Override
    public void saveContact(String address, String firstName, String lastName, String phoneKey) throws Exception {
        saveContact(address, firstName, lastName, phoneKey, null);
    }

    @Override
    public Contact saveGroup() throws Exception {
        return saveGroup(null, null);
    }

    @Override
    public Contact saveUser(Contact contact) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("email", contact.getEmail());
        params.put("firstName", contact.getFirstName());
        params.put("lastName", contact.getLastName());
        params.put("phoneKey", contact.getPhone());

        ServerResponse serverResponse = executeSync(USER_SAVE, params);

        return responseParser.parseContact(serverResponse);
    }

    @Override
    public Contact saveGroup(String type, String advertisement) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        if (type == null) {
            type = "Group";
        }
        if (!type.startsWith("Group")) {
            type = "Group";
        }

        params.put("type", type);

        if (advertisement != null) {
            params.put("advertisement", advertisement);
        }

        return responseParser.parseContact(executeSync(GROUP_SAVE, params));
    }

    @Override
    public void saveContact(String address, String firstName, String lastName, String phoneKey, String notes) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("address", address);
        params.put("firstName", firstName);
        params.put("lastName", lastName);
        params.put("phoneKey", phoneKey);
        if (notes != null) {
            params.put("notes", notes);
        }

        // happens async
        executeSync(CONTACT_SAVE, params);
    }

    @Override
    public void saveContact(String address, String firstName, String lastName, String phoneKey, String notes, String location, String email) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("address", address);
        params.put("firstName", firstName);
        params.put("lastName", lastName);
        params.put("phoneKey", phoneKey);
        if (notes != null) {
            params.put("notes", notes);
        }
        
        if (location != null){
            params.put("loc", location);
        }
        
        if (email != null){
            params.put("email", email);
        }

        // happens async
        executeSync(CONTACT_SAVE, params);
    }

    @Override
    public String getFaceName(String mobileNumber) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("mobileNumber", mobileNumber);

        return responseParser.parseFaceName(executeSync(FACE_NAME, params, false));
    }

    @Override
    public byte[] getFaceImage(String mobileNumber, boolean thumbnail) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("mobileNumber", mobileNumber);
        params.put("thumbnail", thumbnail);

        ObservableFuture<byte[]> binaryResponseFuture = executeAsyncBinaryResponse(FACE_IMAGE, params, false);

        // Block and wait...
        binaryResponseFuture.awaitUninterruptibly();
        return binaryResponseFuture.getResult();
    }

    @Override
    public byte[] getFaceImage(String mobileNumber, int size) throws Exception {

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("mobileNumber", mobileNumber);
        params.put("size", size);
        params.put("thumbnail", true);

        ObservableFuture<byte[]> binaryResponseFuture = executeAsyncBinaryResponse(FACE_IMAGE, params, false);

        // Block and wait...
        binaryResponseFuture.awaitUninterruptibly();
        return binaryResponseFuture.getResult();
    }

    @Override
    public void recordMetricsEvent(ProductLine product, String mobileNumber, String event, String payload) throws Exception {

        if (product == null || StringUtil.isNullOrEmpty(mobileNumber) || StringUtil.isNullOrEmpty(event)) {
            throw new Exception("Missing required parameter.");
        }

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("product", product.toString());
        params.put("mobileNumber", mobileNumber);
        params.put("event", event);

        if (StringUtil.exists(payload)) {
            params.put("payload", payload);
        }

        executeSync(METRICS_EVENT, params);
    }

    @Override
    protected void onDestroy() {

    }

}
