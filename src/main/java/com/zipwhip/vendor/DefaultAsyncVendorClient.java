package com.zipwhip.vendor;

import com.zipwhip.api.Address;
import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.NestedObservableFuture;
import com.zipwhip.api.ZipwhipNetworkSupport;
import com.zipwhip.api.connection.ParameterizedRequest;
import com.zipwhip.api.connection.RequestMethod;
import com.zipwhip.api.dto.*;
import com.zipwhip.api.dto.EnrollmentResult;
import com.zipwhip.api.response.BooleanServerResponse;
import com.zipwhip.api.response.MessageListResult;
import com.zipwhip.api.response.ServerResponse;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.*;
import com.zipwhip.events.Observer;
import com.zipwhip.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;

public class DefaultAsyncVendorClient extends ZipwhipNetworkSupport implements AsyncVendorClient {

    private final Converter<InputStream, EnrollmentResult> enrollmentResultConverter = new Converter<InputStream, EnrollmentResult>() {
        @Override
        public EnrollmentResult convert(InputStream inputStream) throws Exception {
            return responseParser.parseEnrollmentResult(responseParser.parse(StreamUtil.getString(inputStream)));
        }

        @Override
        public InputStream restore(EnrollmentResult enrollmentResult) throws Exception {
            return null;
        }
    };

    /**
     * Create a new {@code DefaultAsyncVendorClient} with a default configuration.
     */
    public DefaultAsyncVendorClient() {

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
    public ObservableFuture<EnrollmentResult> enrollUser(String deviceAddress) {
        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            return executeAsync(
                    RequestMethod.GET,
                    ZipwhipNetworkSupport.USER_ENROLL,
                    new ParameterizedRequest(params),
                    enrollmentResultConverter);


        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> deactivateUser(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            final ObservableFuture<ServerResponse> future = executeAsync(
                    RequestMethod.GET,
                    ZipwhipNetworkSupport.USER_DEACT,
                    new ParameterizedRequest(params),
                    nullStreamConverter);

            return toVoid(future);
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Boolean> userExists(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("mobileNumber", Address.stripToMobileNumber(deviceAddress));

        final ObservableFuture<Boolean> result = new DefaultObservableFuture<Boolean>(this);

        try {
            ObservableFuture<ServerResponse> response = executeAsync(
                    RequestMethod.GET,
                    ZipwhipNetworkSupport.USER_EXISTS,
                    new ParameterizedRequest(params),
                    normalStringResponseConverter);

            response.addObserver(new Observer<ObservableFuture<ServerResponse>>() {
                @Override
                public void notify(Object sender, ObservableFuture<ServerResponse> item) {
                    if (!item.isSuccess()) {
                        result.setFailure(item.getCause());
                        return;
                    }

                    if (item.getResult() instanceof BooleanServerResponse) {
                        result.setSuccess(((BooleanServerResponse) item.getResult()).getResponse());
                        return;
                    }

                    throw new IllegalStateException("Not sure what response was!");
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }

        return result;
    }

    @Override
    public ObservableFuture<Void> suggestCarbon(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            return executeAsyncVoid(ZipwhipNetworkSupport.CARBON_SUGGEST, params);
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Boolean> carbonInstalled(String deviceAddress) {
        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            final ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.CARBON_INSTALLED, params);

            return parse(future, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return getBoolean(future);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Boolean> carbonEnabled(String deviceAddress) {
        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            final ObservableFuture<ServerResponse> future = executeAsync(
                    ZipwhipNetworkSupport.CARBON_ENABLED_VENDOR,
                    params);

            return parse(future, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return getBoolean(future);
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    protected Boolean getBoolean(ObservableFuture<ServerResponse> future) throws Exception {
        if (future.isSuccess()) {
            ServerResponse serverResponse = future.getResult();
            if (serverResponse instanceof BooleanServerResponse) {
                return ((BooleanServerResponse) serverResponse).getResponse();
            }
        } else if (future.isCancelled()) {
            throw new IllegalStateException("Cancelled");
        }

        if (future.getCause() instanceof Exception) {
            throw (Exception)future.getCause();
        }

        throw new Exception(future.getCause());
    }

    @Override
    public ObservableFuture<List<MessageToken>> sendMessage(String deviceAddress, String friendAddress, String body) {
        return sendMessage(deviceAddress, Collections.singleton(friendAddress), body);
    }

    @Override
    public ObservableFuture<List<MessageToken>> sendMessage(String deviceAddress, Set<String> contactMobileNumbers, String body) {
        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        if (CollectionUtil.isNullOrEmpty(contactMobileNumbers)) {
            return invalidArgumentFailureFuture("Must specify at least one contact number");
        }

        Set<String> contactAddresses = new HashSet<String>();

        for (String mobileNumber : contactMobileNumbers) {
            contactAddresses.add(getAddress(mobileNumber));
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("contacts", contactAddresses);
        params.put("body", body);

        try {
            final ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.MESSAGE_SEND, params);

            return parse(future, new Callable<List<MessageToken>>() {
                @Override
                public List<MessageToken> call() throws Exception {
                    return responseParser.parseMessageTokens(future.getResult());
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<MessageListResult> listMessages(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            final ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.MESSAGE_LIST, params);

            return parse(future, new Callable<MessageListResult>() {
                @Override
                public MessageListResult call() throws Exception {
                    return responseParser.parseMessagesListResult(future.getResult());
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<MessageListResult> listMessages(String deviceAddress, final int start, final int limit) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("start", Integer.toString(start));
        params.put("limit", Integer.toString(limit));

        try {
            final ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.MESSAGE_LIST, params);

            return parse(future, new Callable<MessageListResult>() {
                @Override
                public MessageListResult call() throws Exception {
                    MessageListResult result = responseParser.parseMessagesListResult(future.getResult());

                    result.setStart(start);
                    result.setLimit(limit);

                    return result;
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Contact> saveUser(String deviceAddress, Contact user) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        if (user == null) {
            return invalidArgumentFailureFuture("User is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

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
            final ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.USER_SAVE, params);

            return parse(future, new Callable<Contact>() {
                @Override
                public Contact call() throws Exception {
                    return responseParser.parseUserAsContact(future.getResult());
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> readMessages(String deviceAddress, Set<String> ids) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("message", ids);

        try {
            ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.MESSAGE_READ, params);

            return parse(future, NullCallable.getInstance());
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> deleteMessages(String deviceAddress, Set<String> ids) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("message", ids);

        try {
            ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.MESSAGE_DELETE, params);

            return parse(future, NullCallable.getInstance());
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> readConversation(String deviceAddress, String fingerprint) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("fingerprint", fingerprint);

        try {
            ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.CONVERSATION_READ, params);

            return parse(future, NullCallable.getInstance());
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> deleteConversation(String deviceAddress, String fingerprint) {
        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("fingerprint", fingerprint);

        try {
            ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.CONVERSATION_DELETE, params);

            return parse(future, NullCallable.getInstance());
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<List<Conversation>> listConversations(String deviceAddress) {
        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            final ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.CONVERSATION_LIST, params);

            return parse(future, new Callable<List<Conversation>>() {
                @Override
                public List<Conversation> call() throws Exception {
                    return responseParser.parseConversations(future.getResult());
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Contact> saveContact(String deviceAddress, Contact contact) {
        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        } else if (contact == null) {
            return invalidArgumentFailureFuture("Contact is a required argument");
        } else if (StringUtil.isNullOrEmpty(contact.getAddress())) {
            return invalidArgumentFailureFuture("Contact.address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        if (StringUtil.exists(contact.getFirstName())) {
            params.put("firstName", contact.getFirstName());
        }
        if (StringUtil.exists(contact.getLastName())) {
            params.put("lastName", contact.getLastName());
        }
        if (StringUtil.exists(contact.getAddress())) {
            params.put("address", getAddress(contact.getAddress()));
        }
        if (StringUtil.exists(contact.getNotes())) {
            params.put("notes", contact.getNotes());
        }

        try {
            final ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.CONTACT_SAVE, params);

            return parse(future, new Callable<Contact>() {
                @Override
                public Contact call() throws Exception {
                    return responseParser.parseContact(future.getResult());
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Void> deleteContacts(String deviceAddress, Set<String> contactMobileNumbers) {
        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        Set<String> contactAddresses = new HashSet<String>();

        for (String mobileNumber : contactMobileNumbers) {
            contactAddresses.add(getAddress(mobileNumber));
        }

        params.put("contact", contactAddresses);

        try {
            return executeAsyncVoid(ZipwhipNetworkSupport.CONTACT_DELETE, params);
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<List<Contact>> listContacts(String deviceAddress) {

        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceAddress", getDeviceAddress(deviceAddress));

        try {
            final ObservableFuture<ServerResponse> future = executeAsync(ZipwhipNetworkSupport.CONTACT_LIST, params);

            return parse(future, new Callable<List<Contact>>() {
                @Override
                public List<Contact> call() throws Exception {
                    return responseParser.parseContacts(future.getResult());
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public ObservableFuture<Contact> getContact(String deviceAddress, String contactMobileNumber) {
        if (StringUtil.isNullOrEmpty(deviceAddress)) {
            return invalidArgumentFailureFuture("Device address is a required argument");
        } else if (StringUtil.isNullOrEmpty(contactMobileNumber)) {
            return invalidArgumentFailureFuture("Contact mobileNumber is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object>();

        params.put("deviceAddress", getDeviceAddress(deviceAddress));
        params.put("address", getAddress(contactMobileNumber));

        try {
            final ObservableFuture<ServerResponse> future = executeAsync(
                    RequestMethod.GET,
                    ZipwhipNetworkSupport.CONTACT_GET,
                    new ParameterizedRequest(params),
                    normalStringResponseConverter);

            return parse(future, new Callable<Contact>() {
                @Override
                public Contact call() throws Exception {
                    return responseParser.parseContact(future.getResult());
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    protected <T> ObservableFuture<T> parse(final ObservableFuture<ServerResponse> future, final Callable<T> callable) {
        final ObservableFuture<T> result = new NestedObservableFuture<T>(this);

        future.addObserver(new Observer<ObservableFuture<ServerResponse>>() {
            @Override
            public void notify(Object sender, ObservableFuture<ServerResponse> item) {
                try {
                    NestedObservableFuture.syncState(item, result, callable.call());
                } catch (Exception e) {
                    future.setFailure(e);
                }
            }
        });

        return result;
    }

    @Override
    public ObservableFuture<Void> textlineProvision(String phoneNumber) throws Exception {

        if (StringUtil.isNullOrEmpty(phoneNumber)) {
            return invalidArgumentFailureFuture("Phone number is a required argument");
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("phoneNumber", phoneNumber);

        return toVoid(
                executeAsync(
                        RequestMethod.GET,
                        ZipwhipNetworkSupport.TEXTLINE_PROVISION,
                        new ParameterizedRequest(params),
                        nullStreamConverter));

    }

    @Override
    public ObservableFuture<Void> textlineEnroll(String phoneNumber, String email) throws Exception {

        if (StringUtil.isNullOrEmpty(phoneNumber)) {
            return invalidArgumentFailureFuture("Phone number is a required argument");
        }
        if (StringUtil.isNullOrEmpty(email)) {
            return invalidArgumentFailureFuture("Email is a required argument");
        }

        final Map<String, Object> params = new HashMap<String, Object>();

        params.put("phoneNumber", phoneNumber);
        params.put("email", email);

        return toVoid(
                executeAsync(
                        RequestMethod.GET,
                        ZipwhipNetworkSupport.TEXTLINE_ENROLL,
                        new ParameterizedRequest(params),
                        nullStreamConverter));
    }

    @Override
    public ObservableFuture<Void> textlineUnenroll(String phoneNumber) throws Exception {

        if (StringUtil.isNullOrEmpty(phoneNumber)) {
            return invalidArgumentFailureFuture("Phone number is a required argument");
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("phoneNumber", phoneNumber);

        try {
            return toVoid(
                    executeAsync(
                            RequestMethod.GET,
                            ZipwhipNetworkSupport.TEXTLINE_UNENROLL,
                            new ParameterizedRequest(params),
                            nullStreamConverter));
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    private <T> ObservableFuture<T> invalidArgumentFailureFuture(String message) {
        return failureFuture(new IllegalAccessException(message));
    }

    private <T> ObservableFuture<T> failureFuture(Exception e) {
        ObservableFuture<T> future = new DefaultObservableFuture<T>(this);
        future.setFailure(e);
        return future;
    }

    private String getDeviceAddress(String deviceAddress) {
        return Address.encode("device", Address.stripToMobileNumber(deviceAddress), "0");
    }

    private String getAddress(String address) {
        return Address.encode("ptn", Address.stripToMobileNumber(address));
    }

    @Override
    protected void onDestroy() {

    }
}
