package com.zipwhip.vendor;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ZipwhipNetworkSupport;
import com.zipwhip.api.dto.*;
import com.zipwhip.api.dto.EnrollmentResult;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.concurrent.DefaultNetworkFuture;
import com.zipwhip.concurrent.NetworkFuture;
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
     * @param connection The connection to Zipwhip.
     *                   This is mandatory, passing null will result in a {@code IllegalArgumentException} being thrown.
     * @param signalProvider
     */
    public DefaultAsyncVendorClient(ApiConnection connection, SignalProvider signalProvider) {
        super(connection, signalProvider);
    }

    public DefaultAsyncVendorClient(SignalProvider signalProvider) {
        super(signalProvider);
    }

    public DefaultAsyncVendorClient(ApiConnection connection) {
        super(connection);
    }

    @Override
    public NetworkFuture<EnrollmentResult> enrollUser(String mobileNumber) {

        if (StringUtil.isNullOrEmpty(mobileNumber)) {
            return invalidArgumentFailureFuture("Mobile number is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("mobileNumber", mobileNumber);

        try {
            return executeAsync(ZipwhipNetworkSupport.USER_ENROLL, params, true, new InputRunnable<ParsableServerResponse<EnrollmentResult>> () {
                @Override
                public void run(ParsableServerResponse<EnrollmentResult> parsableServerResponse) {
                    // TODO parse
                    parsableServerResponse.getFuture().setSuccess(new EnrollmentResult());
                }
            });
        } catch (Exception e) {
            return failureFuture(e);
        }
    }

    @Override
    public NetworkFuture<Void> deactivateUser(String mobileNumber) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<Boolean> userExists(String mobileNumber) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<Void> suggestCarbon(String mobileNumber) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<List<MessageToken>> sendMessage(String mobileNumber, Message message) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<List<Message>> listMessages(String mobileNumber) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<Contact> saveUser(Contact user) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<Void> readMessages(String mobileNumber, Set<String> uuids) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<Void> deleteMessages(String mobileNumber, Set<String> uuids) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<Void> readConversations(String mobileNumber, Set<String> fingerprints) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<Void> deleteConversations(String mobileNumber, Set<String> uuids) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<List<Conversation>> listConversations(String mobileNumber) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<Contact> saveContact(String mobileNumber, Contact contact) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<Void> deleteContact(String mobileNumber, Set<Long> contactIds) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NetworkFuture<List<Contact>> listContacts(String mobileNumber) {

        if (StringUtil.isNullOrEmpty(mobileNumber)) {
            return invalidArgumentFailureFuture("Mobile number is a required argument");
        }

        Map<String, Object> params = new HashMap<String, Object> ();
        params.put("mobileNumber", mobileNumber);

        try {
            return executeAsync(ZipwhipNetworkSupport.CONTACT_LIST, params, true, new InputRunnable<ParsableServerResponse<List<Contact>>> () {
                @Override
                public void run(ParsableServerResponse<List<Contact>> parsableServerResponse) {
                    // TODO parse
                    parsableServerResponse.getFuture().setSuccess(new ArrayList<Contact>());
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

    @Override
    protected void onDestroy() {

    }

}
