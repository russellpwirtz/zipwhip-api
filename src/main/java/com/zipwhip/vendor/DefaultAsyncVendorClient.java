package com.zipwhip.vendor;

import com.zipwhip.api.Address;
import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ZipwhipNetworkSupport;
import com.zipwhip.api.dto.*;
import com.zipwhip.api.dto.EnrollmentResult;
import com.zipwhip.api.response.BooleanServerResponse;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.concurrent.DefaultNetworkFuture;
import com.zipwhip.concurrent.NetworkFuture;
import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.InputRunnable;
import com.zipwhip.util.StringUtil;

import java.util.*;

public class DefaultAsyncVendorClient extends ZipwhipNetworkSupport implements AsyncVendorClient {

    /**
     * Create a new {@code DefaultAsyncVendorClient} with a default configuration.
     */
    public DefaultAsyncVendorClient() {

    }

    /**
     * Create a new {@code DefaultAsyncVendorClient}
     *
     * @param connection The connection to Zipwhip. This is mandatory, passing null will result in a {@code IllegalArgumentException} being thrown.
     * @param signalProvider A {@code SignalProvider} if your client wants to receive signals via Zipwhip SignalServer.
     */
    public DefaultAsyncVendorClient(ApiConnection connection, SignalProvider signalProvider) {
        super(connection, signalProvider);
    }

    /**
     * Create a new {@code DefaultAsyncVendorClient} with a default connection configuration.
     * The default connection is synchronous.
     *
     * @param signalProvider A {@code SignalProvider} if your client wants to receive signals via Zipwhip SignalServer.
     */
    public DefaultAsyncVendorClient(SignalProvider signalProvider) {
        super(signalProvider);
    }

    /**
     * Create a new {@code DefaultAsyncVendorClient} with the desired connection and no signalProvider.
     *
     * @param connection The connection to Zipwhip. This is mandatory, passing null will result in a {@code IllegalArgumentException} being thrown.
     */
    public DefaultAsyncVendorClient(ApiConnection connection) {
        super(connection);
    }

    @Override
    public NetworkFuture<EnrollmentResult> enrollUser(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.USER_ENROLL, params, true, new InputRunnable<ParsableServerResponse<EnrollmentResult>>() {
                @Override
                public void run(ParsableServerResponse<EnrollmentResult> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseEnrollmentResult(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Void> deactivateUser(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.USER_DEACT, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Boolean> userExists(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));
        params.put("mobileNumber", new Address(deviceAddress).toMobileNumber());

        try {
            return executeAsync(ZipwhipNetworkSupport.USER_EXISTS, params, true, new InputRunnable<ParsableServerResponse<Boolean>>() {
                @Override
                public void run(ParsableServerResponse<Boolean> parsableServerResponse) {

                    if (parsableServerResponse.getServerResponse() instanceof BooleanServerResponse) {
                        parsableServerResponse.getFuture().setSuccess(((BooleanServerResponse) parsableServerResponse.getServerResponse()).getResponse());
                    } else {
                         parsableServerResponse.getFuture().setFailure(new Exception("Bad server response type"));
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Void> suggestCarbon(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.CARBON_SUGGEST, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Boolean> carbonInstalled(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.CARBON_INSTALLED, params, true, new InputRunnable<ParsableServerResponse<Boolean>>() {
                @Override
                public void run(ParsableServerResponse<Boolean> parsableServerResponse) {

                    if (parsableServerResponse.getServerResponse() instanceof BooleanServerResponse) {
                        parsableServerResponse.getFuture().setSuccess(((BooleanServerResponse) parsableServerResponse.getServerResponse()).getResponse());
                    } else {
                         parsableServerResponse.getFuture().setFailure(new Exception("Bad server response type"));
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Boolean> carbonEnabled(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.CARBON_ENABLED_VENDOR, params, true, new InputRunnable<ParsableServerResponse<Boolean>>() {
                @Override
                public void run(ParsableServerResponse<Boolean> parsableServerResponse) {

                    if (parsableServerResponse.getServerResponse() instanceof BooleanServerResponse) {
                        parsableServerResponse.getFuture().setSuccess(((BooleanServerResponse) parsableServerResponse.getServerResponse()).getResponse());
                    } else {
                         parsableServerResponse.getFuture().setFailure(new Exception("Bad server response type"));
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<List<MessageToken>> sendMessage(String deviceAddress, Set<String> contactMobileNumbers, String body) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        if (CollectionUtil.isNullOrEmpty(contactMobileNumbers)) {
            return invalidArgumentFailureFuture("Must specify at least one contact number");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));
        params.put("contacts", contactMobileNumbers);
        params.put("body", body);

        try {
            return executeAsync(ZipwhipNetworkSupport.MESSAGE_SEND, params, true, new InputRunnable<ParsableServerResponse<List<MessageToken>>> () {
                @Override
                public void run(ParsableServerResponse<List<MessageToken>> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseMessageTokens(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<List<Message>> listMessages(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.MESSAGE_LIST, params, true, new InputRunnable<ParsableServerResponse<List<Message>>> () {
                @Override
                public void run(ParsableServerResponse<List<Message>> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseMessages(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Contact> saveUser(String deviceAddress, Contact user) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        if (user == null) {
            return invalidArgumentFailureFuture("User is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));

        if (StringUtil.exists(user.getFirstName())) {
            params.put("firstName", user.getFirstName());
        }
        if (StringUtil.exists(user.getLastName())) {
            params.put("lastName", user.getLastName());
        }
        if (StringUtil.exists(user.getNotes())) {
            params.put("notes", user.getNotes());
        }

        try {
            return executeAsync(ZipwhipNetworkSupport.USER_SAVE, params, true, new InputRunnable<ParsableServerResponse<Contact>> () {
                @Override
                public void run(ParsableServerResponse<Contact> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseUser(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Void> readMessages(String deviceAddress, Set<String> uuids) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));
        params.put("uuid", uuids);

        try {
            return executeAsync(ZipwhipNetworkSupport.MESSAGE_READ, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Void> deleteMessages(String deviceAddress, Set<String> uuids) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));
        params.put("uuids", uuids);

        try {
            return executeAsync(ZipwhipNetworkSupport.MESSAGE_DELETE, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Void> readConversation(String deviceAddress, String fingerprint) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));
        params.put("fingerprint", fingerprint);

        try {
            return executeAsync(ZipwhipNetworkSupport.CONVERSATION_READ, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Void> deleteConversation(String deviceAddress, String fingerprint) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));
        params.put("fingerprint", fingerprint);

        try {
            return executeAsync(ZipwhipNetworkSupport.CONVERSATION_DELETE, params, true, new InputRunnable<ParsableServerResponse<Void>>() {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<List<Conversation>> listConversations(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.CONVERSATION_LIST, params, true, new InputRunnable<ParsableServerResponse<List<Conversation>>> () {
                @Override
                public void run(ParsableServerResponse<List<Conversation>> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseConversations(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Contact> saveContact(String deviceAddress, Contact contact) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        if (contact == null) {
            return invalidArgumentFailureFuture("Contact is a required argument");
        }

        if (StringUtil.isNullOrEmpty(contact.getAddress())) {
            return invalidArgumentFailureFuture("Contact.address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));

        if (StringUtil.exists(contact.getFirstName())) {
            params.put("firstName", contact.getFirstName());
        }
        if (StringUtil.exists(contact.getLastName())) {
            params.put("lastName", contact.getLastName());
        }
        if (StringUtil.exists(contact.getAddress())) {
            params.put("address", contact.getAddress());
        }
        if (StringUtil.exists(contact.getMobileNumber())) {
            params.put("mobileNumber", contact.getMobileNumber());
        }
        if (StringUtil.exists(contact.getNotes())) {
            params.put("notes", contact.getNotes());
        }

        try {
            return executeAsync(ZipwhipNetworkSupport.CONTACT_SAVE, params, true, new InputRunnable<ParsableServerResponse<Contact>> () {
                @Override
                public void run(ParsableServerResponse<Contact> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseContact(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Void> deleteContacts(String deviceAddress, Set<String> contactAddresses) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));
        params.put("contact", contactAddresses);

        try {
            return executeAsync(ZipwhipNetworkSupport.CONTACT_DELETE, params, true, new InputRunnable<ParsableServerResponse<Void>> () {
                @Override
                public void run(ParsableServerResponse<Void> parsableServerResponse) {
                    parsableServerResponse.getFuture().setSuccess(null);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<List<Contact>> listContacts(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));

        try {
            return executeAsync(ZipwhipNetworkSupport.CONTACT_LIST, params, true, new InputRunnable<ParsableServerResponse<List<Contact>>> () {
                @Override
                public void run(ParsableServerResponse<List<Contact>> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseContacts(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Contact> getContact(String deviceAddress, String contactMobileNumber) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }
        if (StringUtil.isNullOrEmpty(contactMobileNumber)) {
            return invalidArgumentFailureFuture("Contact mobileNumber is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("deviceAddress", validateOrTransform(deviceAddress));
        params.put("mobileNumber", new Address(deviceAddress).toMobileNumber());

        try {
            return executeAsync(ZipwhipNetworkSupport.CONTACT_GET, params, true, new InputRunnable<ParsableServerResponse<Contact>> () {
                @Override
                public void run(ParsableServerResponse<Contact> parsableServerResponse) {
                    try {
                        parsableServerResponse.getFuture().setSuccess(responseParser.parseContact(parsableServerResponse.getServerResponse()));
                    } catch (Exception e) {
                        parsableServerResponse.getFuture().setFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    private <T> NetworkFuture<T> invalidArgumentFailureFuture(String message) {
        return failureFuture(new IllegalAccessException(message));
    }

    private <T> NetworkFuture<T> failureFuture(Exception e) {
        NetworkFuture<T> future = new DefaultNetworkFuture<T>(this);
        future.setFailure(e);
        return future;
    }

    private String validateOrTransform(String deviceAddress) {
        Address address = new Address(deviceAddress);
        return address.toDeviceAddress();
    }

    @Override
    protected void onDestroy() {

    }

}
