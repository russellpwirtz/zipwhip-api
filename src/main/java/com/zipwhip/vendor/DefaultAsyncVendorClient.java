package com.zipwhip.vendor;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ZipwhipNetworkSupport;
import com.zipwhip.api.dto.Contact;
import com.zipwhip.api.dto.Conversation;
import com.zipwhip.api.dto.Message;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.api.response.EnrollmentResult;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.concurrent.NetworkFuture;

import java.util.List;
import java.util.Set;

public class DefaultAsyncVendorClient extends ZipwhipNetworkSupport implements AsyncVendorClient {

    /**
     *
     */
    public DefaultAsyncVendorClient() {

    }

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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

//    @Override
//    public NetworkFuture<Boolean> userSubscribe(String mobileNumber, SubscriptionEntry subscriptionEntry) throws Exception {
//
//        Map<String, Object> params = new HashMap<String, Object>();
//
//        if (subscriptionEntry != null){
//            // TODO: add params
//            params.put("signalFilters", subscriptionEntry.getSignalFilters());
//        }
//
//        return executeAsync("user/subscribe", params, true, new InputRunnable<ParsableServerResponse<Boolean>>() {
//
//            @Override
//            public void run(ParsableServerResponse<Boolean> p) {
//                // TODO: add protection from crashes
//                // this will execute in the "callbackExecutor"
//                p.getFuture().setSuccess(((BooleanServerResponse) p.getServerResponse()).getResponse());
//            }
//        });
//
//    }

    @Override
    protected void onDestroy() {

    }

}
