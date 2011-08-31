package com.zipwhip.api;

import com.zipwhip.api.dto.Contact;
import com.zipwhip.api.dto.Message;
import com.zipwhip.api.dto.MessageStatus;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.sockets.SocketSignalProvider;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.FakeFuture;
import com.zipwhip.lib.Address;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.util.StringUtil;
import org.apache.log4j.Logger;

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
public class DefaultZipwhipClient extends ZipwhipNetworkSupport implements ZipwhipClient {

    private static Logger logger = Logger.getLogger(DefaultZipwhipClient.class);

    public DefaultZipwhipClient() {
        this(new HttpConnection(), new SocketSignalProvider());
    }

    public DefaultZipwhipClient(Connection connection) {
        this(connection, new SocketSignalProvider());
    }

    public DefaultZipwhipClient(final Connection connection, final SignalProvider signalProvider) {

        super(connection, signalProvider);

        getSignalProvider().onNewClientIdReceived(new Observer<String>() {

            @Override
            public void notify(Object sender, String clientId) {

                if (StringUtil.isNullOrEmpty(clientId)) {
                    logger.warn("Received CONNECT without clientId");
                    return;
                }

                // lets do a signals connect!
                Map<String, Object> params = new HashMap<String, Object>();

                params.put("clientId", clientId);
                params.put("sessions", connection.getSessionKey());

                try {
                    executeSync("signals/connect", params);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public Future<Boolean> connect() throws Exception {

        if (connection == null) {
            throw new NullPointerException("The connection can not be null");
        }

        if (signalProvider == null) {
            throw new NullPointerException("The signalProvider cannot be null");
        }

        // we need to determine if we're authenticated enough
        if (!connection.isConnected() || !connection.isAuthenticated()) {
            throw new Exception("The connection cannot operate at this time");
        }

        if (!signalProvider.isConnected()) {
            // this will NOT block until you're connected
            // it's asynchronous
            return signalProvider.connect();
        }

        return new FakeFuture<Boolean>(true);
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

        for (String address : addresses) {
            params.put("contacts", address);
        }

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
    public Message getMessage(String uuid) throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("uuid", uuid);

        return responseParser.parseMessage(executeSync(MESSAGE_GET, params));
    }

    @Override
    public MessageStatus getMessageStatus(String uuid) throws Exception {
        Message message = getMessage(uuid);
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

        // cplx.response.optJSONObject("user")
        // TODO: this will crash. i need to fix build errors and will revisit.
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

    //    public void requestSockets(final String clientId) throws Exception {

    //    }
    //
    //        });
    //            }
    //                return connection.requestSockets(requestBuilder.build());
    //            public String run() {
    //
    //        ServerResponse> serverResponse = execute(new StringRunnable() {
    //
    //        }
    //            params.put("index", String.valueOf(index));
    //        if (index != -1) {
    //
    //        }
    //            params.put("subscriptionId", subscriptionId);
    //        if (subscriptionId != null) {
    //
    //        params.put("clientId", clientId);
    //
    //        final RequestBuilder requestBuilder = new RequestBuilder();
    //    public void requestSockets(final String clientId, final String subscriptionId, final int index) throws Exception {
    //
    //    }
    //        requestSockets(clientId, null, -1);

    @Override
    protected void onDestroy() {

    }
    
}
